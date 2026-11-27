package com.elementsplus.core.file;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 每个 {@link MemoryFileSystem.WriteResult.Failure} 都必须有可显示的错误文案。
 *
 * <p>界面上直接使用 {@code failureKey()}，一旦新增失败原因却忘了加语言键，
 * 玩家会看到原始键名。</p>
 */
class FileErrorKeysTest {

    @Test
    void everyFailureHasTranslationInBothLanguages() {
        JsonObject zh = loadLang("zh_cn");
        JsonObject en = loadLang("en_us");
        for (MemoryFileSystem.WriteResult.Failure failure : MemoryFileSystem.WriteResult.Failure.values()) {
            if (failure == MemoryFileSystem.WriteResult.Failure.NONE) {
                continue;
            }
            String key = new MemoryFileSystem.WriteResult(false, failure).failureKey();
            assertTrue(zh.has(key), () -> "zh_cn 缺少错误文案: " + key);
            assertTrue(en.has(key), () -> "en_us 缺少错误文案: " + key);
        }
    }

    private static JsonObject loadLang(String locale) {
        String path = "/assets/elements-plus/lang/" + locale + ".json";
        InputStream stream = FileErrorKeysTest.class.getResourceAsStream(path);
        assertNotNull(stream, "找不到语言文件: " + path);
        JsonElement parsed = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        return parsed.getAsJsonObject();
    }
}
