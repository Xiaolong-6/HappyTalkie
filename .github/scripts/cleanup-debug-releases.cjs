// Reserved CI tags only. A merged PR and an Actions-authored prerelease are
// required before deleting a release; formal releases are never touched.
module.exports = async function cleanup({ github, context, core }) {
  const repo = context.repo;
  const releases = await github.paginate(github.rest.repos.listReleases, { ...repo, per_page: 100 });
  const refs = await github.paginate(github.rest.git.listMatchingRefs, { ...repo, ref: 'tags/debug-pr-' });
  const byTag = new Map(releases.map(release => [release.tag_name, release]));
  const tags = new Set([...byTag.keys(), ...refs.map(ref => ref.ref.replace(/^refs\/tags\//, ''))]);
  for (const tag of tags) {
    const match = /^debug-pr-([1-9]\d*)$/.exec(tag);
    if (!match) continue;
    const release = byTag.get(tag);
    if (release && (!release.prerelease || release.draft || release.author?.login !== 'github-actions[bot]')) continue;
    let pr;
    try {
      ({ data: pr } = await github.rest.pulls.get({ ...repo, pull_number: Number(match[1]) }));
    } catch (error) {
      if (error.status === 404) continue;
      throw error;
    }
    if (!pr.merged_at || pr.base.ref !== 'main' || pr.head.repo?.full_name !== `${repo.owner}/${repo.repo}`) continue;
    // Delete release first. Any error except "already absent" prevents tag deletion.
    if (release) {
      try {
        await github.rest.repos.deleteRelease({ ...repo, release_id: release.id });
      } catch (error) {
        if (error.status !== 404) throw error;
      }
    }
    try {
      await github.rest.git.deleteRef({ ...repo, ref: `tags/${tag}` });
    } catch (error) {
      if (error.status !== 404) throw error;
    }
    core.info(`Removed merged PR debug distribution: ${tag}`);
  }
};
