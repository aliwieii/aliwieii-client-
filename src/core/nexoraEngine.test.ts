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
