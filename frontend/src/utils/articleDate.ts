export const getArticleDate = (date?: string, fallback?: string) => date || fallback || '';

export const formatArticleDate = (date?: string, fallback?: string) => {
  const target = getArticleDate(date, fallback);
  if (!target) return '';

  return new Date(target).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  });
};