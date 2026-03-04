/**
 * Bookmark management — localStorage backed.
 * A bookmark stores: articleId, title, savedAt, readingProgress.
 */

const STORAGE_KEY = 'article-bookmarks';

export interface BookmarkEntry {
  articleId: number;
  title: string;
  summary: string;
  savedAt: string; // ISO date
  progress: number; // 0-100
}

function load(): BookmarkEntry[] {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
  } catch {
    return [];
  }
}

function save(bookmarks: BookmarkEntry[]) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(bookmarks));
}

export function getBookmarks(): BookmarkEntry[] {
  return load().sort((a, b) => new Date(b.savedAt).getTime() - new Date(a.savedAt).getTime());
}

export function isBookmarked(articleId: number): boolean {
  return load().some(b => b.articleId === articleId);
}

export function addBookmark(entry: Omit<BookmarkEntry, 'savedAt'>): void {
  const list = load().filter(b => b.articleId !== entry.articleId);
  list.push({ ...entry, savedAt: new Date().toISOString() });
  save(list);
}

export function removeBookmark(articleId: number): void {
  save(load().filter(b => b.articleId !== articleId));
}

export function toggleBookmark(entry: Omit<BookmarkEntry, 'savedAt'>): boolean {
  if (isBookmarked(entry.articleId)) {
    removeBookmark(entry.articleId);
    return false; // removed
  }
  addBookmark(entry);
  return true; // added
}

export function getBookmarkCount(): number {
  return load().length;
}
