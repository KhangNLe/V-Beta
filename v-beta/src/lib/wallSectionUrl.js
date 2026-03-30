/** Canonical slug for /wall/{slug}/{id} — keep in sync everywhere. */
export function wallSectionSlugFromName(name) {
  if (!name || typeof name !== "string") return "";
  return encodeURIComponent(name.replace(/\s+/g, "-"));
}

export function wallSectionPath(section) {
  const slug = wallSectionSlugFromName(section.wall_section_name);
  return `/wall/${slug}/${section.wall_section_id}`;
}
