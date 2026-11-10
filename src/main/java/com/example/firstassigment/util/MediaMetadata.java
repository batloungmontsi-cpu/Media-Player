package com.example.firstassigment.util;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Minimal, dependency-free reader for ID3 metadata (artist + embedded cover art)
 * plus cover images stored alongside the tracks. Every method is defensive and
 * returns {@code null} rather than throwing.
 */
public final class MediaMetadata {

    private MediaMetadata() {}

    private static final String[] COVER_NAMES = {
            "cover.jpg", "cover.jpeg", "cover.png",
            "folder.jpg", "folder.jpeg", "folder.png",
            "album.jpg", "albumart.jpg", "front.jpg", "artwork.jpg", "artwork.png"
    };

    /** Standard ID3v1 genre list, indexed by the byte stored at offset 127. */
    private static final String[] ID3V1_GENRES = {
            "Blues", "Classic Rock", "Country", "Dance", "Disco", "Funk", "Grunge", "Hip-Hop",
            "Jazz", "Metal", "New Age", "Oldies", "Other", "Pop", "R&B", "Rap", "Reggae", "Rock",
            "Techno", "Industrial", "Alternative", "Ska", "Death Metal", "Pranks", "Soundtrack",
            "Euro-Techno", "Ambient", "Trip-Hop", "Vocal", "Jazz+Funk", "Fusion", "Trance",
            "Classical", "Instrumental", "Acid", "House", "Game", "Sound Clip", "Gospel", "Noise",
            "Alternative Rock", "Bass", "Soul", "Punk", "Space", "Meditative", "Instrumental Pop",
            "Instrumental Rock", "Ethnic", "Gothic", "Darkwave", "Techno-Industrial", "Electronic",
            "Pop-Folk", "Eurodance", "Dream", "Southern Rock", "Comedy", "Cult", "Gangsta", "Top 40",
            "Christian Rap", "Pop/Funk", "Jungle", "Native American", "Cabaret", "New Wave",
            "Psychedelic", "Rave", "Showtunes", "Trailer", "Lo-Fi", "Tribal", "Acid Punk",
            "Acid Jazz", "Polka", "Retro", "Musical", "Rock & Roll", "Hard Rock", "Folk",
            "Folk-Rock", "National Folk", "Swing", "Fast Fusion", "Bebob", "Latin", "Revival",
            "Celtic", "Bluegrass", "Avantgarde", "Gothic Rock", "Progressive Rock",
            "Psychedelic Rock", "Symphonic Rock", "Slow Rock", "Big Band", "Chorus", "Easy Listening",
            "Acoustic", "Humour", "Speech", "Chanson", "Opera", "Chamber Music", "Sonata",
            "Symphony", "Booty Bass", "Primus", "Porn Groove", "Satire", "Slow Jam", "Club",
            "Tango", "Samba", "Folklore", "Ballad", "Power Ballad", "Rhythmic Soul", "Freestyle",
            "Duet", "Punk Rock", "Drum Solo", "A Cappella", "Euro-House", "Dance Hall", "Goa",
            "Drum & Bass", "Club-House", "Hardcore", "Terror", "Indie", "BritPop", "Negerpunk",
            "Polsk Punk", "Beat", "Christian Gangsta Rap", "Heavy Metal", "Black Metal", "Crossover",
            "Contemporary Christian", "Christian Rock", "Merengue", "Salsa", "Thrash Metal", "Anime",
            "JPop", "Synthpop"
    };

    /** Reads the first artist found: ID3v2 (TPE1) then ID3v1 fallback. */
    public static String readArtist(File file) {
        String artist = readId3v2Artist(file);
        if (artist == null || artist.isBlank()) artist = readId3v1Artist(file);
        return (artist == null || artist.isBlank()) ? null : artist.trim();
    }

    /** Extracts embedded cover art (ID3v2 APIC) as raw image bytes, or null. */
    public static byte[] readArtwork(File file) {
        return readId3v2Artwork(file);
    }

    /** Looks for a cover image stored in the album folder. */
    public static File findFolderImage(File folder) {
        if (folder == null || !folder.isDirectory()) return null;
        for (String name : COVER_NAMES) {
            File f = new File(folder, name);
            if (f.isFile()) return f;
        }
        File[] images = folder.listFiles((d, n) -> {
            String l = n.toLowerCase();
            return l.endsWith(".jpg") || l.endsWith(".jpeg") || l.endsWith(".png");
        });
        return (images != null && images.length > 0) ? images[0] : null;
    }

    /* ---------- combined tags ---------- */

    /** Title / album / artist / year / track / genre read from one media file; any field may be {@code null}. */
    public static final class Tags {
        public final String title;
        public final String album;
        public final String artist;
        public final String year;
        public final String track;
        public final String genre;

        public Tags(String title, String album, String artist, String year, String track, String genre) {
            this.title = title;
            this.album = album;
            this.artist = artist;
            this.year = year;
            this.track = track;
            this.genre = genre;
        }
    }

    /** Reads the common ID3 frames in a single pass, with ID3v1 fallback for the missing text fields. */
    public static Tags readTags(File file) {
        String[] out = new String[6];
        Id3v2 tag = readId3v2(file);
        eachFrame(tag, (id, data) -> {
            switch (id) {
                case "TIT2" -> out[0] = blankToNull(out[0], decodeText(data));
                case "TALB" -> out[1] = blankToNull(out[1], decodeText(data));
                case "TPE1", "TPE2" -> out[2] = blankToNull(out[2], decodeText(data));
                case "TDRC", "TYER" -> out[3] = blankToNull(out[3], yearFrom(decodeText(data)));
                case "TRCK" -> out[4] = blankToNull(out[4], trackFrom(decodeText(data)));
                case "TCON" -> out[5] = blankToNull(out[5], decodeText(data));
                default -> { }
            }
            return false;
        });

        if (out[0] == null || out[1] == null || out[2] == null || out[3] == null) {
            String[] v1 = readId3v1(file);
            if (out[0] == null) out[0] = v1[0];
            if (out[1] == null) out[1] = v1[1];
            if (out[2] == null) out[2] = v1[2];
            if (out[3] == null) out[3] = v1[3];
            if (out[5] == null) out[5] = v1[4];
        }
        return new Tags(out[0], out[1], out[2], out[3], out[4], out[5]);
    }

    /** Pulls the leading 4-digit year out of a free-form date such as {@code 2021-05-01} or {@code 2021}. */
    private static String yearFrom(String value) {
        if (value == null) return null;
        String s = value.trim();
        for (int i = 0; i + 4 <= s.length(); i++) {
            String chunk = s.substring(i, i + 4);
            if (chunk.chars().allMatch(Character::isDigit)) return chunk;
        }
        return s.isEmpty() ? null : s;
    }

    /** Keeps only the leading track number from a value such as {@code 3/12}. */
    private static String trackFrom(String value) {
        if (value == null) return null;
        int slash = value.indexOf('/');
        String s = (slash >= 0 ? value.substring(0, slash) : value).trim();
        return s.isEmpty() ? null : s;
    }

    private static String blankToNull(String current, String value) {
        if (current != null) return current;
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    /** ID3v1 fallback tag: {title, album, artist, year, genre}. */
    private static String[] readId3v1(File file) {
        String[] out = new String[5];
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long length = raf.length();
            if (length < 128) return out;
            raf.seek(length - 128);
            byte[] tag = new byte[128];
            raf.readFully(tag);
            if (tag[0] != 'T' || tag[1] != 'A' || tag[2] != 'G') return out;
            out[0] = cleanV1(tag, 3);
            out[2] = cleanV1(tag, 33);
            out[1] = cleanV1(tag, 63);
            out[3] = cleanV1(tag, 93, 4);
            out[4] = genreV1(tag[127] & 0xFF);
        } catch (Exception e) {
            // ignore
        }
        return out;
    }

    private static String genreV1(int code) {
        if (code < 0 || code >= ID3V1_GENRES.length) return null;
        String g = ID3V1_GENRES[code];
        return (g == null || g.isBlank()) ? null : g;
    }

    private static String cleanV1(byte[] tag, int offset) {
        return cleanV1(tag, offset, 30);
    }

    private static String cleanV1(byte[] tag, int offset, int length) {
        if (offset < 0 || offset + length > tag.length) return null;
        String s = new String(tag, offset, length, StandardCharsets.ISO_8859_1).replace("\u0000", "").trim();
        return s.isEmpty() ? null : s;
    }

    /* ---------- ID3v2 ---------- */

    private static final class Id3v2 {
        int version;
        byte[] body;
    }

    private interface FrameSink {
        boolean frame(String id, byte[] data);
    }

    private static Id3v2 readId3v2(File file) {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            if (raf.length() < 10) return null;
            byte[] header = new byte[10];
            raf.readFully(header);
            if (header[0] != 'I' || header[1] != 'D' || header[2] != '3') return null;

            Id3v2 tag = new Id3v2();
            tag.version = header[3] & 0xFF;
            int size = ((header[6] & 0x7F) << 21)
                    | ((header[7] & 0x7F) << 14)
                    | ((header[8] & 0x7F) << 7)
                    | (header[9] & 0x7F);
            if (size <= 0 || size > 20_000_000) return null;

            byte[] body = new byte[size];
            raf.readFully(body);
            tag.body = body;
            return tag;
        } catch (Exception e) {
            return null;
        }
    }

    private static void eachFrame(Id3v2 tag, FrameSink sink) {
        if (tag == null || tag.body == null) return;
        byte[] b = tag.body;
        int pos = 0;
        while (pos + 10 <= b.length) {
            if (b[pos] == 0) break;
            String id = new String(b, pos, 4, StandardCharsets.ISO_8859_1);

            int size;
            if (tag.version >= 4) {
                size = ((b[pos + 4] & 0x7F) << 21)
                        | ((b[pos + 5] & 0x7F) << 14)
                        | ((b[pos + 6] & 0x7F) << 7)
                        | (b[pos + 7] & 0x7F);
            } else {
                size = ((b[pos + 4] & 0xFF) << 24)
                        | ((b[pos + 5] & 0xFF) << 16)
                        | ((b[pos + 6] & 0xFF) << 8)
                        | (b[pos + 7] & 0xFF);
            }
            if (size <= 0 || pos + 10 + size > b.length) break;

            byte[] data = new byte[size];
            System.arraycopy(b, pos + 10, data, 0, size);
            if (sink.frame(id, data)) return;
            pos += 10 + size;
        }
    }

    private static String readId3v2Artist(File file) {
        Id3v2 tag = readId3v2(file);
        String[] out = new String[1];
        eachFrame(tag, (id, data) -> {
            if ("TPE1".equals(id) || "TPE2".equals(id)) {
                out[0] = decodeText(data);
                return out[0] != null;
            }
            return false;
        });
        return out[0];
    }

    private static byte[] readId3v2Artwork(File file) {
        Id3v2 tag = readId3v2(file);
        byte[][] out = new byte[1][];
        eachFrame(tag, (id, data) -> {
            if ("APIC".equals(id)) {
                out[0] = parseApic(data);
                return out[0] != null;
            }
            return false;
        });
        return out[0];
    }

    private static String decodeText(byte[] data) {
        if (data.length < 2) return null;
        int enc = data[0] & 0xFF;
        Charset cs = switch (enc) {
            case 1 -> StandardCharsets.UTF_16;
            case 2 -> StandardCharsets.UTF_16BE;
            case 3 -> StandardCharsets.UTF_8;
            default -> StandardCharsets.ISO_8859_1;
        };
        String s = new String(data, 1, data.length - 1, cs);
        return s.replace("\u0000", "").trim();
    }

    private static byte[] parseApic(byte[] d) {
        if (d.length < 4) return null;
        int pos = 0;
        int enc = d[pos++] & 0xFF;

        while (pos < d.length && d[pos] != 0) pos++; // MIME type
        pos++;
        if (pos >= d.length) return null;
        pos++; // picture type

        if (enc == 1 || enc == 2) {
            while (pos + 1 < d.length && !(d[pos] == 0 && d[pos + 1] == 0)) pos += 2;
            pos += 2;
        } else {
            while (pos < d.length && d[pos] != 0) pos++;
            pos++;
        }
        if (pos >= d.length) return null;

        byte[] image = new byte[d.length - pos];
        System.arraycopy(d, pos, image, 0, image.length);
        return image.length > 0 ? image : null;
    }

    /* ---------- ID3v1 ---------- */

    private static String readId3v1Artist(File file) {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long length = raf.length();
            if (length < 128) return null;
            raf.seek(length - 128);
            byte[] tag = new byte[128];
            raf.readFully(tag);
            if (tag[0] != 'T' || tag[1] != 'A' || tag[2] != 'G') return null;
            return new String(tag, 33, 30, StandardCharsets.ISO_8859_1).trim();
        } catch (Exception e) {
            return null;
        }
    }
}
