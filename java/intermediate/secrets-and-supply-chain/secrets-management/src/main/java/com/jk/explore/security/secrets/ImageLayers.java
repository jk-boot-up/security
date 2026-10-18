package com.jk.explore.security.secrets;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * A container image modelled as a stack of layer archives. A later layer can hide a file with a "whiteout" marker,
 * but the earlier layer is still in the image, and anyone who has the image can unpack it.
 */
final class ImageLayers {

    static final String WHITEOUT = ".wh.";

    private final List<byte[]> layers = new ArrayList<>();

    void addLayer(Map<String, String> files) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Map.Entry<String, String> file : files.entrySet()) {
                ZipEntry entry = new ZipEntry(file.getKey());
                entry.setTime(0);
                zip.putNextEntry(entry);
                zip.write(file.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        layers.add(out.toByteArray());
    }

    /** Deletes a file in a new layer, the only way a layered image can: the old layer is unchanged. */
    void deleteInNewLayer(String path) throws IOException {
        int slash = path.lastIndexOf('/');
        String marker = path.substring(0, slash + 1) + WHITEOUT + path.substring(slash + 1);
        addLayer(Map.of(marker, ""));
    }

    int layerCount() {
        return layers.size();
    }

    /** What a running container sees: the layers applied in order, with whiteouts removing files. */
    Map<String, String> mergedView() throws IOException {
        Map<String, String> view = new LinkedHashMap<>();
        for (byte[] layer : layers) {
            for (Map.Entry<String, String> file : unpack(layer).entrySet()) {
                String name = file.getKey();
                int slash = name.lastIndexOf('/');
                String base = name.substring(slash + 1);
                if (base.startsWith(WHITEOUT)) {
                    view.remove(name.substring(0, slash + 1) + base.substring(WHITEOUT.length()));
                } else {
                    view.put(name, file.getValue());
                }
            }
        }
        return view;
    }

    /** What anyone with the image file can read: every file of every layer. */
    Map<String, String> layer(int index) throws IOException {
        return unpack(layers.get(index));
    }

    private static Map<String, String> unpack(byte[] layer) throws IOException {
        Map<String, String> files = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(layer))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                files.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return files;
    }
}
