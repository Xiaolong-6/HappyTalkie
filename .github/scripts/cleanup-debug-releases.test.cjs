const { test } = require('node:test');
const assert = require('node:assert/strict');
const cleanup = require('./cleanup-debug-releases.cjs');
const release = (tag, extra = {}) => ({ id: 13, tag_name: tag, prerelease: true, author: {login: 'github-actions[bot]'}, ...extra });
async function run({ releases = [release('debug-pr-13')], tags = ['debug-pr-13'], merged = true, head = 'owner/repo', base = 'main', fail, getError } = {}) {
  const calls = [];
  const github = { rest: { repos: {
    listReleases: 'releases',
    deleteRelease: async () => { calls.push('release'); if (fail) throw { status: fail }; },
  }, git: {
    listMatchingRefs: 'refs', deleteRef: async ({ref}) => { calls.push(ref); },
  }, pulls: { get: async () => {
    if (getError) throw { status: getError };
    return {data: { merged_at: merged ? '2026-09-27' : null, head: { repo: { full_name: head } }, base: {ref: base} }};
  } } }, paginate: async (method) => method === 'releases' ? releases : tags.map(t => ({ref:`refs/tags/${t}`})) };
  await cleanup({github, context: {repo: {owner: 'owner', repo: 'repo'}}, core: {info() {}}});
  return calls;
}
test('merged PR: release is deleted before its tag', async () => assert.deepEqual(await run(), ['release', 'tags/debug-pr-13']));
test('open or unmerged closed PR is retained', async () => assert.deepEqual(await run({merged: false}), []));
test('main and version releases are retained', async () => assert.deepEqual(await run({releases:[release('debug-main'),release('v1.0')],tags:['debug-main','v1.0']}), []));
test('formal release on reserved tag is retained', async () => assert.deepEqual(await run({releases:[release('debug-pr-13',{prerelease:false})]}), []));
test('human-created release is retained', async () => assert.deepEqual(await run({releases:[release('debug-pr-13',{author:{login:'owner'}})]}), []));
test('draft is retained', async () => assert.deepEqual(await run({releases:[release('debug-pr-13',{draft:true})]}), []));
test('fork is retained', async () => assert.deepEqual(await run({head:'fork/repo'}), []));
test('non-main target is retained', async () => assert.deepEqual(await run({base:'develop'}), []));
test('orphan reserved tag of merged PR is removed', async () => assert.deepEqual(await run({releases:[]}), ['tags/debug-pr-13']));
test('already absent release still allows tag cleanup', async () => assert.deepEqual(await run({fail:404}), ['release','tags/debug-pr-13']));
test('permission/server errors propagate', async () => assert.rejects(run({fail:403})));
test('unknown PR is retained', async () => assert.deepEqual(await run({getError:404}), []));
test('lookup permission error propagates', async () => assert.rejects(run({getError:403})));
test('only exact reserved tags match', async () => assert.deepEqual(await run({releases:[], tags:['debug-pr-13-backup','debug-pr-0','debug-pr-013']}), []));
test('empty repository is a no-op', async () => assert.deepEqual(await run({releases:[],tags:[]}), []));

test('release without a tag is removed without deleting an absent ref', async () => assert.deepEqual(await run({tags:[]}), ['release']));
