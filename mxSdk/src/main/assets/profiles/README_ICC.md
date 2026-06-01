# ICC配置文件使用说明

## 已下载的配置文件

### RGB色彩空间
- `sRGB.icc` - 标准RGB色彩空间（最常用）
- `AdobeRGB.icc` - Adobe RGB色彩空间（更广色域）
- `ECI-RGB-v2.icc` - 欧洲色彩倡议RGB标准
- `ProPhotoRGB.icc` - 专业摄影RGB色彩空间
- `Display.icc` - 显示器配置文件

### CMYK色彩空间
- `USWebCoatedSWOP.icc` - 美国标准印刷CMYK
- `CoatedFOGRA39.icc` - 欧洲标准印刷CMYK
- `JapanColor2001Coated.icc` - 日本标准印刷CMYK
- `UncoatedFOGRA29.icc` - 欧洲非涂层印刷CMYK

### 设备配置文件
- `Printer.icc` - 打印机配置文件

## 使用方法

### Android项目集成
1. 确保文件位于 `app/src/main/assets/profiles/` 目录
2. 在代码中通过AssetManager访问：

```java
AssetManager assetManager = getAssets();
InputStream is = assetManager.open("profiles/sRGB.icc");
```

### Little CMS使用示例
```c
cmsHPROFILE hInProfile = cmsOpenProfileFromFile("sRGB.icc", "r");
cmsHPROFILE hOutProfile = cmsOpenProfileFromFile("USWebCoatedSWOP.icc", "r");
cmsHTRANSFORM hTransform = cmsCreateTransform(hInProfile, TYPE_RGB_8,
                                            hOutProfile, TYPE_CMYK_8,
                                            INTENT_PERCEPTUAL, 0);
```

## 验证方法

确保文件是真正的二进制ICC格式：
- 使用 `file` 命令检查：`file *.icc`
- 文件大小应该在200KB-500KB之间
- 文件开头应该是二进制数据，不是文本

