import workflowYml from '../../.github/workflows/build.yml?raw';
import buildGradle from '../../forge-1.8.9/build.gradle?raw';
import settingsGradle from '../../forge-1.8.9/settings.gradle?raw';
import gradleProperties from '../../forge-1.8.9/gradle.properties?raw';
import gradleWrapperProperties from '../../forge-1.8.9/gradle/wrapper/gradle-wrapper.properties?raw';
import gradlewScript from '../../forge-1.8.9/gradlew?raw';
import mcmodInfo from '../../forge-1.8.9/src/main/resources/mcmod.info?raw';
import mixinsJson from '../../forge-1.8.9/src/main/resources/mixins.nexora.json?raw';
import nexoraClientJava from '../../forge-1.8.9/src/main/java/com/nexora/client/core/NexoraClient.java?raw';
import arabicRendererJava from '../../forge-1.8.9/src/main/java/com/nexora/client/localization/ArabicTextRenderer.java?raw';
import i18nManagerJava from '../../forge-1.8.9/src/main/java/com/nexora/client/localization/I18nManager.java?raw';
import gl4esRenderJava from '../../forge-1.8.9/src/main/java/com/nexora/client/utils/GL4ESRenderUtils.java?raw';
import moduleCategoryJava from '../../forge-1.8.9/src/main/java/com/nexora/client/modules/ModuleCategory.java?raw';
import moduleJava from '../../forge-1.8.9/src/main/java/com/nexora/client/modules/Module.java?raw';
import hudModuleJava from '../../forge-1.8.9/src/main/java/com/nexora/client/hud/HudModule.java?raw';
import moduleManagerJava from '../../forge-1.8.9/src/main/java/com/nexora/client/modules/ModuleManager.java?raw';
import configManagerJava from '../../forge-1.8.9/src/main/java/com/nexora/client/config/ConfigManager.java?raw';
import profileManagerJava from '../../forge-1.8.9/src/main/java/com/nexora/client/profiles/ProfileManager.java?raw';
import guiNexoraMainJava from '../../forge-1.8.9/src/main/java/com/nexora/client/gui/GuiNexoraMain.java?raw';
import guiHudEditorJava from '../../forge-1.8.9/src/main/java/com/nexora/client/gui/GuiHudEditor.java?raw';
import mixinEntityRendererJava from '../../forge-1.8.9/src/main/java/com/nexora/client/mixin/MixinEntityRenderer.java?raw';
import mixinGuiIngameJava from '../../forge-1.8.9/src/main/java/com/nexora/client/mixin/MixinGuiIngame.java?raw';
import mixinRendererLivingJava from '../../forge-1.8.9/src/main/java/com/nexora/client/mixin/MixinRendererLivingEntity.java?raw';

export interface ForgeProjectFile {
  path: string;
  language: 'java' | 'gradle' | 'json' | 'yaml' | 'properties';
  roleAr: string;
  roleEn: string;
  content: string;
}

export const FORGE_PROJECT_FILES: ForgeProjectFile[] = [
  {
    path: '.github/workflows/build.yml',
    language: 'yaml',
    roleAr: 'سير عمل GitHub Actions مخصص لبناء مود Forge 1.8.9 فقط عبر actions/setup-java@v3 (Java 8) وتنفيذ ./gradlew clean build ورفع NEXORA-CLIENT-*.jar.',
    roleEn: 'GitHub Actions workflow dedicated to forge-1.8.9 using actions/setup-java@v3 (java-version: 8), ./gradlew clean build, and actions/upload-artifact@v4.',
    content: workflowYml,
  },
  {
    path: 'forge-1.8.9/gradlew',
    language: 'gradle',
    roleAr: 'سكربت تشغيل Gradle Wrapper مع ربط تلقائي لـ JDK 8 Toolchain وبيئة تشغيل Loom.',
    roleEn: 'POSIX Gradle Wrapper launcher script preserving JDK 8 toolchain while launching Loom daemon.',
    content: gradlewScript,
  },
  {
    path: 'forge-1.8.9/build.gradle',
    language: 'gradle',
    roleAr: 'إعدادات البناء (Essential Loom 0.10.0.5 + Architectury Pack200 0.1.3 + Mixin 0.7.11 + Java 8 Toolchain).',
    roleEn: 'Essential Loom 0.10.0.5 + Architectury Pack200 0.1.3 + SpongePowered Mixin 0.7.11 build script with Java 8 Toolchain.',
    content: buildGradle,
  },
  {
    path: 'forge-1.8.9/settings.gradle',
    language: 'gradle',
    roleAr: 'مستودعات Maven الرسمية لـ Forge و SpongePowered و Architectury/Essential.',
    roleEn: 'Plugin management repositories for Forge, SpongePowered, and Essential Loom.',
    content: settingsGradle,
  },
  {
    path: 'forge-1.8.9/gradle.properties',
    language: 'properties',
    roleAr: 'خصائص إصدار المود وذاكرة JVM واكتشاف مسارات JDK 8 و JDK 17 تلقائياً.',
    roleEn: 'Project properties defining Minecraft 1.8.9, Forge 11.15.1.2318, Mixin 0.7.11, and JDK toolchain env discovery.',
    content: gradleProperties,
  },
  {
    path: 'forge-1.8.9/gradle/wrapper/gradle-wrapper.properties',
    language: 'properties',
    roleAr: 'إعدادات Gradle Wrapper الرسمية المثبتة على الإصدار 7.6.4 المتوافق مع Essential Loom.',
    roleEn: 'Gradle Wrapper configuration pinned to Gradle 7.6.4.',
    content: gradleWrapperProperties,
  },
  {
    path: 'forge-1.8.9/src/main/resources/mcmod.info',
    language: 'json',
    roleAr: 'بيانات تعريف المود الرسمية داخل Minecraft Forge 1.8.9.',
    roleEn: 'Forge 1.8.9 mod metadata descriptor.',
    content: mcmodInfo,
  },
  {
    path: 'forge-1.8.9/src/main/resources/mixins.nexora.json',
    language: 'json',
    roleAr: 'تسجيل كلاسات Mixin 0.7.11 لتعديل EntityRenderer و GuiIngame و RendererLivingEntity.',
    roleEn: 'SpongePowered Mixin 0.7.11 configuration targeting 1.8.9 client classes.',
    content: mixinsJson,
  },
  {
    path: 'forge-1.8.9/src/main/java/com/nexora/client/core/NexoraClient.java',
    language: 'java',
    roleAr: 'نقطة الدخول الرئيسية (@Mod) وإدارة دورة الحياة والاختصارات وحفظ الإعدادات عند الإغلاق.',
    roleEn: 'Main @Mod entry point managing lifecycle, keybindings (RSHIFT, H), and shutdown persistence.',
    content: nexoraClientJava,
  },
  {
    path: 'forge-1.8.9/src/main/java/com/nexora/client/localization/ArabicTextRenderer.java',
    language: 'java',
    roleAr: 'معالج النصوص العربية (RTL & Shaping) باستخدام ICU4J المدمجة في 1.8.9 مع ذاكرة تخزين مؤقت LRU.',
    roleEn: 'ICU4J ArabicShaping & Bidi text processor with LRU caching for zero per-frame allocations.',
    content: arabicRendererJava,
  },
  {
    path: 'forge-1.8.9/src/main/java/com/nexora/client/localization/I18nManager.java',
    language: 'java',
    roleAr: 'نظام الترجمة الفوري بين العربية (RTL) والإنجليزية (LTR) دون إعادة تشغيل اللعبة.',
    roleEn: 'Runtime bilingual localization manager (Arabic RTL & English LTR).',
    content: i18nManagerJava,
  },
  {
    path: 'forge-1.8.9/src/main/java/com/nexora/client/utils/GL4ESRenderUtils.java',
    language: 'java',
    roleAr: 'محرك الرسم المتوافق مع PojavLauncher (GL4ES / Adreno 710) عبر Tessellator و WorldRenderer دون Shaders.',
    roleEn: 'GL4ES-safe rendering utility using Tessellator, WorldRenderer, and trigonometry lookup tables.',
    content: gl4esRenderJava,
  },
  {
    path: 'forge-1.8.9/src/main/java/com/nexora/client/modules/ModuleManager.java',
    language: 'java',
    roleAr: 'مدير الميزات وعناصر HUD مع مخزن حلقي (Ring Buffer) للـ CPS وتحديثات متباعدة للنصوص.',
    roleEn: 'Central module & HUD manager with primitive CPS ring buffer and throttled telemetry.',
    content: moduleManagerJava,
  },
  {
    path: 'forge-1.8.9/src/main/java/com/nexora/client/hud/HudModule.java',
    language: 'java',
    roleAr: 'الكلاس الأساسي لعناصر HUD القابلة للسحب وتغيير الحجم والقفل ومنع الخروج عن حدود الشاشة.',
    roleEn: 'Base HUD widget class supporting scale, opacity, lock, and screen bounds clamping.',
    content: hudModuleJava,
  },
  {
    path: 'forge-1.8.9/src/main/java/com/nexora/client/modules/Module.java',
    language: 'java',
    roleAr: 'الكلاس الأساسي لكل ميزة مع إدارة التسجيل في EventBus وإعادة الضبط.',
    roleEn: 'Base feature module class handling Forge EventBus registration and state.',
    content: moduleJava,
  },
  {
    path: 'forge-1.8.9/src/main/java/com/nexora/client/modules/ModuleCategory.java',
    language: 'java',
    roleAr: 'تصنيفات الميزات (HUD, PVP, VISUALS, INVENTORY, PERFORMANCE) باللغتين.',
    roleEn: 'Bilingual module category enumeration.',
    content: moduleCategoryJava,
  },
  {
    path: 'forge-1.8.9/src/main/java/com/nexora/client/config/ConfigManager.java',
    language: 'java',
    roleAr: 'نظام حفظ واسترجاع الإعدادات بصيغة JSON خارج خيط الرسم مع معالجة الملفات التالفة.',
    roleEn: 'Schema-versioned JSON configuration manager with async off-thread disk writes and corruption recovery.',
    content: configManagerJava,
  },
  {
    path: 'forge-1.8.9/src/main/java/com/nexora/client/profiles/ProfileManager.java',
    language: 'java',
    roleAr: 'إدارة ملفات التعريف (Practice, Bed Wars, General PvP, Performance, Custom).',
    roleEn: 'Multi-profile manager protecting active profile integrity.',
    content: profileManagerJava,
  },
  {
    path: 'forge-1.8.9/src/main/java/com/nexora/client/gui/GuiNexoraMain.java',
    language: 'java',
    roleAr: 'الواجهة الرئيسية داخل اللعبة مع الشريط الجانبي ودعم RTL والتمرير السلس.',
    roleEn: 'In-game main dashboard screen with RTL/LTR sidebar, scissor scrolling, and module cards.',
    content: guiNexoraMainJava,
  },
  {
    path: 'forge-1.8.9/src/main/java/com/nexora/client/gui/GuiHudEditor.java',
    language: 'java',
    roleAr: 'شاشة محرر الواجهة (HUD Editor) للسحب والمحاذاة الشبكية والقفل داخل اللعبة.',
    roleEn: 'Interactive in-game HUD Editor with grid snapping, locking, and reset controls.',
    content: guiHudEditorJava,
  },
  {
    path: 'forge-1.8.9/src/main/java/com/nexora/client/mixin/MixinEntityRenderer.java',
    language: 'java',
    roleAr: 'حقن EntityRenderer للتحكم في شدة اهتزاز الشاشة عند الضرر (HurtCam).',
    roleEn: 'Mixin into EntityRenderer.hurtCameraEffect for customizable HurtCam reduction.',
    content: mixinEntityRendererJava,
  },
  {
    path: 'forge-1.8.9/src/main/java/com/nexora/client/mixin/MixinGuiIngame.java',
    language: 'java',
    roleAr: 'حقن GuiIngame لرسم عناصر HUD الخاصة بـ NEXORA بأداء خفيف.',
    roleEn: 'Mixin into GuiIngame.renderGameOverlay to render NEXORA HUD widgets.',
    content: mixinGuiIngameJava,
  },
  {
    path: 'forge-1.8.9/src/main/java/com/nexora/client/mixin/MixinRendererLivingEntity.java',
    language: 'java',
    roleAr: 'حقن RendererLivingEntity لتخصيص لون إصابة الخصم (HitColor).',
    roleEn: 'Mixin into RendererLivingEntity for custom HitColor tinting.',
    content: mixinRendererLivingJava,
  },
];
