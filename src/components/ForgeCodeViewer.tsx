import React, { useState } from 'react';
import { FORGE_PROJECT_FILES, ForgeProjectFile } from '../core/forgeSourceCatalog';
import { Copy, Check, Download, FileCode, Terminal, FolderGit2 } from 'lucide-react';

interface ForgeCodeViewerProps {
  lang: 'ar' | 'en';
  generatedConfigJson: string;
  activeProfileName: string;
}

export const ForgeCodeViewer: React.FC<ForgeCodeViewerProps> = ({
  lang,
  generatedConfigJson,
  activeProfileName,
}) => {
  const isRtl = lang === 'ar';
  const [selectedPath, setSelectedPath] = useState<string>(FORGE_PROJECT_FILES[0].path);
  const [copiedPath, setCopiedPath] = useState<string | null>(null);

  const allFiles: ForgeProjectFile[] = [
    ...FORGE_PROJECT_FILES,
    {
      path: `forge-1.8.9/run/nexora/profile_${activeProfileName.replace(/\s+/g, '_')}.json`,
      language: 'json',
      roleAr: 'ملف الإعدادات الحي المولد من تعديلاتك الحالية في الواجهة ومحرر HUD.',
      roleEn: 'Live configuration JSON generated from your current UI and HUD Editor customizations.',
      content: generatedConfigJson,
    },
  ];

  const activeFile = allFiles.find((f) => f.path === selectedPath) || allFiles[0];

  const handleCopy = async (content: string, path: string) => {
    try {
      await navigator.clipboard.writeText(content);
      setCopiedPath(path);
      setTimeout(() => setCopiedPath(null), 2000);
    } catch {
      // Fallback ignored
    }
  };

  const handleDownloadSingle = (file: ForgeProjectFile) => {
    const blob = new Blob([file.content], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = file.path.split('/').pop() || 'file.txt';
    a.click();
    URL.revokeObjectURL(url);
  };

  const handleDownloadFullBundleScript = () => {
    // Generates a self-extracting POSIX shell script that recreates the entire Forge 1.8.9 project & GitHub Actions workflow locally
    const lines: string[] = [
      '#!/usr/bin/env bash',
      '# NEXORA CLIENT 1.0.0 (Forge 1.8.9) — Complete Repository Generator',
      'set -e',
      'echo "Creating NEXORA CLIENT Forge 1.8.9 project structure..."',
    ];
    for (const f of allFiles) {
      const dir = f.path.includes('/') ? f.path.substring(0, f.path.lastIndexOf('/')) : '.';
      lines.push(`mkdir -p "${dir}"`);
      lines.push(`cat << 'EOF_NEXORA_FILE' > "${f.path}"`);
      lines.push(f.content);
      lines.push('EOF_NEXORA_FILE');
    }
    lines.push('echo "Done! Push to GitHub or run: cd forge-1.8.9 && gradle build"');

    const blob = new Blob([lines.join('\n')], { type: 'text/x-shellscript;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'install-nexora-forge-1.8.9-repo.sh';
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="space-y-5">
      {/* Header Banner */}
      <div className="bg-[#111621] border border-[#21262D] rounded-lg p-4 flex flex-wrap items-center justify-between gap-4">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <FolderGit2 className="w-5 h-5 text-[#00A8FF]" />
            <h2 className="text-base font-bold text-[#E6EDF3]">
              {isRtl
                ? 'الملفات البرمجية الكاملة لمشروع Forge 1.8.9 + GitHub Actions'
                : 'Complete Forge 1.8.9 Java Source Code & GitHub Actions CI'}
            </h2>
          </div>
          <p className="text-xs text-[#8B949E]">
            {isRtl
              ? 'جميع الكلاسات مكتوبة بالكامل بدون أي اختصار (Zero TODOs) ومتوافقة مع SpongePowered Mixin 0.7.11 و GL4ES و ICU4J ArabicShaping.'
              : 'All 21 files are written in full without placeholders (Zero TODOs), targeting Forge 11.15.1.2318, Mixin 0.7.11, GL4ES, and ICU4J ArabicShaping.'}
          </p>
        </div>

        <button
          type="button"
          onClick={handleDownloadFullBundleScript}
          className="inline-flex items-center gap-2 px-4 py-2 rounded-md bg-[#00A8FF] text-[#0B0E14] text-xs font-semibold hover:bg-[#33B9FF] transition-colors whitespace-nowrap shrink-0"
        >
          <Download className="w-4 h-4" />
          <span>
            {isRtl
              ? 'تحميل حزمة المستودع الكاملة (.sh)'
              : 'Download Complete Repo Installer (.sh)'}
          </span>
        </button>
      </div>

      {/* File Tree + Source Viewer */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-4 items-start">
        {/* Left File Tree (4 cols) */}
        <div className="lg:col-span-4 bg-[#111621] border border-[#21262D] rounded-lg p-3 space-y-1.5 max-h-[620px] overflow-y-auto">
          <div className="px-2 py-1 text-xs font-mono text-[#8B949E] border-b border-[#21262D] mb-2 flex items-center justify-between" dir="ltr">
            <span>Repository Files ({allFiles.length})</span>
            <span>Java 8 / Forge</span>
          </div>
          {allFiles.map((file) => {
            const active = file.path === activeFile.path;
            return (
              <button
                key={file.path}
                type="button"
                onClick={() => setSelectedPath(file.path)}
                dir="ltr"
                className={`w-full text-left px-3 py-2 rounded-md text-xs font-mono flex items-center gap-2 transition-colors ${
                  active
                    ? 'bg-[#00A8FF]/15 text-[#00A8FF] border border-[#00A8FF]/40 font-semibold'
                    : 'text-[#E6EDF3] hover:bg-[#161B22] border border-transparent'
                }`}
              >
                <FileCode className="w-3.5 h-3.5 shrink-0 text-[#00A8FF]" />
                <span className="truncate">{file.path}</span>
              </button>
            );
          })}
        </div>

        {/* Right Code Viewer (8 cols) */}
        <div className="lg:col-span-8 bg-[#111621] border border-[#21262D] rounded-lg overflow-hidden">
          <div className="bg-[#161B22] border-b border-[#21262D] px-4 py-3 flex flex-wrap items-center justify-between gap-3">
            <div className="space-y-0.5">
              <div className="text-xs font-mono font-semibold text-[#00A8FF]" dir="ltr">
                {activeFile.path}
              </div>
              <div className="text-xs text-[#8B949E]">
                {isRtl ? activeFile.roleAr : activeFile.roleEn}
              </div>
            </div>

            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => handleCopy(activeFile.content, activeFile.path)}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded bg-[#0B0E14] border border-[#21262D] text-xs font-medium text-[#E6EDF3] hover:border-[#00A8FF] transition-colors whitespace-nowrap shrink-0"
              >
                {copiedPath === activeFile.path ? (
                  <>
                    <Check className="w-3.5 h-3.5 text-[#3FB950]" />
                    <span>{isRtl ? 'تم النسخ' : 'Copied'}</span>
                  </>
                ) : (
                  <>
                    <Copy className="w-3.5 h-3.5 text-[#00A8FF]" />
                    <span>{isRtl ? 'نسخ الكود الكامل' : 'Copy Full File'}</span>
                  </>
                )}
              </button>

              <button
                type="button"
                onClick={() => handleDownloadSingle(activeFile)}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded bg-[#0B0E14] border border-[#21262D] text-xs font-medium text-[#E6EDF3] hover:border-[#A371F7] transition-colors whitespace-nowrap shrink-0"
              >
                <Download className="w-3.5 h-3.5 text-[#A371F7]" />
                <span>{isRtl ? 'تنزيل الملف' : 'Download File'}</span>
              </button>
            </div>
          </div>

          <pre
            dir="ltr"
            className="p-4 text-xs font-mono text-[#E6EDF3] bg-[#0B0E14] overflow-x-auto max-h-[540px] leading-relaxed"
          >
            <code>{activeFile.content}</code>
          </pre>
        </div>
      </div>

      {/* PojavLauncher & GitHub Actions Verification Guide */}
      <div className="bg-[#111621] border border-[#21262D] rounded-lg p-4 space-y-3">
        <div className="flex items-center gap-2 text-sm font-semibold text-[#E6EDF3]">
          <Terminal className="w-4 h-4 text-[#3FB950]" />
          <span>
            {isRtl
              ? 'خطوات البناء عبر GitHub Actions والتثبيت على PojavLauncher (Moto G Stylus 5G / Adreno 710)'
              : 'GitHub Actions Build & PojavLauncher Installation Guide (Moto G Stylus 5G / Adreno 710)'}
          </span>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs text-[#8B949E]">
          <div className="bg-[#0B0E14] border border-[#21262D] rounded p-3 space-y-1">
            <div className="font-semibold text-[#E6EDF3]">
              {isRtl ? '1. بناء ملف الـ JAR تلقائياً (JDK 17 + JDK 8)' : '1. Automated JAR Build (JDK 17 + JDK 8)'}
            </div>
            <p>
              {isRtl
                ? 'ارفع مجلد forge-1.8.9 وملف .github/workflows/build.yml إلى مستودع GitHub. يشغّل GitHub Actions بيئة Gradle 7.6.4 على JDK 17 (لتشغيل Essential Loom و Pack200) ويستخدم JDK 8 Toolchain لترجمة المود إلى Java 8 Bytecode (Major 52) وإنتاج NEXORA-CLIENT-1.0.0-1.8.9.jar.'
                : 'Push forge-1.8.9 and .github/workflows/build.yml to GitHub. GitHub Actions runs Gradle 7.6.4 on JDK 17 (for Essential Loom & Pack200) while compiling mod classes via JDK 8 Toolchain into Java 8 bytecode (Major 52) inside NEXORA-CLIENT-1.0.0-1.8.9.jar.'}
            </p>
          </div>
          <div className="bg-[#0B0E14] border border-[#21262D] rounded p-3 space-y-1">
            <div className="font-semibold text-[#E6EDF3]">
              {isRtl ? '2. التثبيت داخل PojavLauncher' : '2. PojavLauncher Setup'}
            </div>
            <p>
              {isRtl
                ? 'ثبت Forge 1.8.9 (11.15.1.2318) في PojavLauncher، ثم انسخ ملف NEXORA-CLIENT-1.0.0-1.8.9.jar إلى المسار .minecraft/mods/1.8.9/ مع اختيار Java 8 ومحرك الرندر Holy GL4ES.'
                : 'Install Forge 11.15.1.2318 in PojavLauncher, place NEXORA-CLIENT-1.0.0-1.8.9.jar inside .minecraft/mods/1.8.9/, and select Java 8 with Holy GL4ES.'}
            </p>
          </div>
          <div className="bg-[#0B0E14] border border-[#21262D] rounded p-3 space-y-1">
            <div className="font-semibold text-[#E6EDF3]">
              {isRtl ? '3. طريقة الاسترجاع الآمن (Rollback)' : '3. Safe Rollback Procedure'}
            </div>
            <p>
              {isRtl
                ? 'جميع إعدادات NEXORA تحفظ في مجلد مستقل (.minecraft/nexora/) دون المساس بملف options.txt الأصلي. لإلغاء المود يكفي حذف ملف الـ JAR من مجلد mods.'
                : 'All NEXORA configs live in an isolated .minecraft/nexora/ directory without overwriting vanilla options.txt. Remove the JAR from mods/ to roll back cleanly.'}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};
