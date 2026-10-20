import test from 'node:test';
import assert from 'node:assert/strict';
import {
  createInitialModules,
  createDefaultProfiles,
  filterModules,
  clampHudPosition,
  snapToGrid,
  autoArrangeHudModules,
  deleteProfileSafely,
  parseAndValidateConfig,
  HITDELAYFIX_REFERENCE_AUDIT,
  FORENSIC_FEATURE_MATRIX,
  analyzeFrameTimeHistory,
  computeParticleStudioBurst,
  evaluateBridgePitchAlignment,
} from './nexoraEngine.ts';

test('createInitialModules initializes all 83 unique NEXORA modules', () => {
  const modules = createInitialModules();
  assert.equal(modules.length, 83);
  const ids = new Set(modules.map((m) => m.id));
  assert.equal(ids.size, 83);
});

test('filterModules searches Arabic and English titles and filters by category/status', () => {
  const modules = createInitialModules();
  const arabicSearch = filterModules(modules, 'عداد النقرات', 'ALL', 'ALL');
  assert.equal(arabicSearch.length, 1);
  assert.equal(arabicSearch[0].id, 'cps');

  const pvpEnabled = filterModules(modules, '', 'PVP', 'ENABLED');
  assert.ok(pvpEnabled.length > 0);
  assert.ok(pvpEnabled.every((m) => m.category === 'PVP' && m.enabled));
});

test('clampHudPosition prevents HUD elements from escaping screen boundaries', () => {
  const clampedNegative = clampHudPosition(-50, -30, 100, 40, 1.0, 800, 450);
  assert.equal(clampedNegative.x, 4);
  assert.equal(clampedNegative.y, 4);

  const clampedOverflow = clampHudPosition(950, 500, 100, 40, 1.5, 800, 450);
  assert.equal(clampedOverflow.x, 800 - 150 - 4);
  assert.equal(clampedOverflow.y, 450 - 60 - 4);
});

test('snapToGrid rounds coordinates to nearest grid increment', () => {
  assert.equal(snapToGrid(19, 8), 16);
  assert.equal(snapToGrid(21, 8), 24);
});

test('autoArrangeHudModules respects locked HUD elements while arranging unlocked ones', () => {
  const modules = createInitialModules();
  const fpsIdx = modules.findIndex((m) => m.id === 'fps');
  modules[fpsIdx] = { ...modules[fpsIdx], x: 333, y: 222, locked: true };

  const arranged = autoArrangeHudModules(modules, 768, 432);
  const lockedFps = arranged.find((m) => m.id === 'fps')!;
  assert.equal(lockedFps.x, 333);
  assert.equal(lockedFps.y, 222);
});

test('deleteProfileSafely switches active profile before deleting and prevents deleting last profile', () => {
  const modules = createInitialModules();
  const profiles = createDefaultProfiles(modules);
  assert.equal(profiles.length, 5);

  const res = deleteProfileSafely(profiles, 'bedwars', 'bedwars');
  assert.equal(res.deleted, true);
  assert.equal(res.profiles.length, 4);
  assert.notEqual(res.activeProfileId, 'bedwars');

  // Cannot delete when only 1 profile remains
  const single = [res.profiles[0]];
  const failRes = deleteProfileSafely(single, single[0].id, single[0].id);
  assert.equal(failRes.deleted, false);
  assert.equal(failRes.profiles.length, 1);
});

test('parseAndValidateConfig recovers gracefully from corrupted JSON without crashing', () => {
  const modules = createInitialModules();
  const corrupted = '{ "schemaVersion": 1, "modules": BROKEN_JSON ';
  const result = parseAndValidateConfig(corrupted, modules);
  assert.equal(result.valid, false);
  assert.equal(result.recoveredFromError, true);
  assert.equal(result.modules.length, 83);
});

test('HITDELAYFIX_REFERENCE_AUDIT classifies adopted, GL4ES-adapted, and rejected unsafe classes', () => {
  assert.ok(HITDELAYFIX_REFERENCE_AUDIT.length >= 7);
  const adopted = HITDELAYFIX_REFERENCE_AUDIT.filter((e) => e.decision === 'ADOPTED_CLEAN');
  const adapted = HITDELAYFIX_REFERENCE_AUDIT.filter((e) => e.decision === 'ADAPTED_GL4ES');
  const rejected = HITDELAYFIX_REFERENCE_AUDIT.filter((e) => e.decision === 'REJECTED_UNSAFE');
  assert.ok(adopted.length >= 4);
  assert.ok(adapted.length >= 1);
  assert.ok(rejected.length >= 2);
});

test('analyzeFrameTimeHistory computes average FPS, 1% low FPS, max spike, and stutter count (>33.3ms)', () => {
  const samples = [10, 10, 11, 10, 12, 10, 11, 10, 42.5, 10];
  const result = analyzeFrameTimeHistory(samples);
  assert.equal(result.stutterEvents, 1);
  assert.equal(result.maxSpikeMs, 42.5);
  assert.ok(result.avgFps > 60);
  assert.ok(result.onePercentLowFps < result.avgFps);
});

test('computeParticleStudioBurst enforces budget cap and throttles on GL4ES frame spikes (>28ms)', () => {
  const normal = computeParticleStudioBurst({
    enabled: true,
    safeMode: false,
    multiplier: 4,
    budgetCapPerSec: 80,
    emittedThisSecond: 20,
    currentFrameTimeMs: 11.2,
    cullOnSpike: true,
  });
  assert.equal(normal.emittedNow, 4);
  assert.equal(normal.throttledBySpike, false);

  const spiked = computeParticleStudioBurst({
    enabled: true,
    safeMode: false,
    multiplier: 5,
    budgetCapPerSec: 80,
    emittedThisSecond: 20,
    currentFrameTimeMs: 34.0,
    cullOnSpike: true,
  });
  assert.equal(spiked.emittedNow, 1);
  assert.equal(spiked.throttledBySpike, true);
});

test('evaluateBridgePitchAlignment classifies Ninja, Godbridge, and Moonwalk angles without input automation', () => {
  assert.equal(evaluateBridgePitchAlignment(78.5, 45).mode, 'NINJA / FAST');
  assert.equal(evaluateBridgePitchAlignment(75.8, 180).mode, 'GODBRIDGE / FLAT');
  assert.equal(evaluateBridgePitchAlignment(40.0, 90).isOptimal, false);
});

test('FORENSIC_FEATURE_MATRIX maps core HUD, Performance, Particle, and Builder systems', () => {
  assert.ok(FORENSIC_FEATURE_MATRIX.length >= 10);
  assert.ok(FORENSIC_FEATURE_MATRIX.every((f) => f.gl4esSafe));
});

