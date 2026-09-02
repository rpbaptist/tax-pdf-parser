
### Dev workflow

For all new features and changes:

- Use `grill-with-docs` skill before doing anything. Claude Code's built-in plan
  mode is not a substitute, even if it produced a design doc or a plan file —
  run `grill-with-docs` explicitly regardless of whether plan mode was used.
- Record my prompts
- Create an artifact for the resulting plan from step 1.
- Record the plan as an issue in markdown, with an `Artifact:` link at the top.
- Create a GitHub issue with the plan.
- Implement new issues on a new worktree.
- When changing or adding new functionality, use `tdd` skill.
- Before committing, use `qa` skill. CI also runs `fallow` (see below) in
  audit mode, so a local `npm run check:fallow` catches the same gate early.
- Create logically grouped, atomic commits.
- Update documentation according to changes made.
- Create a PR and review it. Concise, only comment when something requires attention and is actionable.
- Address small review issues directly. If larger or makes sense to follow up, create a new GitHub issues.

Default to continuing to the next step unless instructed otherwise.

### Commit style

1. Limit the subject line to 70 characters
2. Separate subject from body with a blank line
3. Capitalize the subject line
4. Do not end the subject line with a period
5. Use the imperative mood in the subject line
6. Use the body to explain what and why vs. how

