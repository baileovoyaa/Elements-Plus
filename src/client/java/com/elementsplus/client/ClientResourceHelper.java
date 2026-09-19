package com.elementsplus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;



public class ClientResourceHelper {

    /**
     * 在路径字符串中的文件名（不含扩展名）后追加指定后缀。
     *
     * <p>示例：
     * <pre>
     *   addSuffixToFileName("textures/gui/background.png", ".zh_cn")
     *       -> "textures/gui/background.zh_cn.png"
     *   addSuffixToFileName("D:\\a\\b\\model.json", "_bak")
     *       -> "D:\\a\\b\\model_bak.json"
     *   addSuffixToFileName("textures/gui/", ".zh_cn")
     *       -> "textures/gui/"          // 路径以分隔符结尾，原样返回
     *   addSuffixToFileName("LICENSE", ".zh_cn")
     *       -> "LICENSE.zh_cn"          // 无扩展名
     * </pre>
     *
     * @param path   原始路径（可含 '/' 或 '\' 分隔符）
     * @param suffix 要插入的后缀，例如 ".zh_cn"；为 null 或空串时原样返回
     * @return 处理后的路径
     */
    public static String addSuffixToFileName(String path, String suffix) {
        if (path == null || path.isEmpty() || suffix == null || suffix.isEmpty()) {
            return path;
        }

        // 1. 找到最后一个路径分隔符（兼容 Windows 的 '\'）
        int sepIndex = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));

        String dir = path.substring(0, sepIndex + 1); // 含分隔符，例如 "textures/gui/"
        String fileName = path.substring(sepIndex + 1);

        // 路径以分隔符结尾（如 "textures/gui/"），没有文件名，原样返回
        if (fileName.isEmpty()) {
            return path;
        }

        // 2. 在文件名中找最后一个 '.'，注意以 '.' 开头的隐藏文件（如 .gitignore）
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0) {
            // 没有扩展名，或整个文件名就是一个点文件
            return dir + fileName + suffix;
        }

        // 3. 在扩展名之前插入后缀
        return dir
                + fileName.substring(0, dotIndex)
                + suffix
                + fileName.substring(dotIndex);
    }

    /**
     * 获取当前语言对应的纹理路径，若不存在则返回默认路径
     */
    public static ResourceLocation getLocalizedTexture(ResourceLocation defaultPath) {
        Minecraft mc = Minecraft.getInstance();
        String language = mc.getLanguageManager().getSelected(); // 如 "zh_cn"

        // 1. 构造带语言代码的路径
        ResourceLocation localizedLoc = ResourceLocation.tryBuild(defaultPath.getNamespace(), addSuffixToFileName(defaultPath.getPath(), "." + language));

        // 2. 检测该路径的资源是否存在
        if (localizedLoc != null && mc.getResourceManager().getResource(localizedLoc).isPresent()) {
            return localizedLoc;
        }

        // 3. 回退到默认路径
        return defaultPath;
    }
}