import { useState, useEffect, useCallback } from 'react';
import type { Article, PageResponse } from '../types';
import { articleApi } from '../services';

interface UseArticlesOptions {
  page?: number;
  size?: number;
  categoryId?: number;
  tagId?: number;
  keyword?: string;
}

interface UseArticlesResult {
  articles: Article[];
  isLoading: boolean;
  error: string | null;
  totalPages: number;
  currentPage: number;
  hasMore: boolean;
  refresh: () => void;
  loadMore: () => void;
}

export const useArticles = (options: UseArticlesOptions = {}): UseArticlesResult => {
  const { page = 0, size = 10, categoryId, tagId, keyword } = options;

  const [articles, setArticles] = useState<Article[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [pageInfo, setPageInfo] = useState({ totalPages: 0, currentPage: 0, hasMore: false });

  const fetchArticles = useCallback(
    async (pageNum: number, append = false) => {
      setIsLoading(true);
      setError(null);

      try {
        let response;

        if (keyword) {
          response = await articleApi.search(keyword, pageNum, size);
        } else if (categoryId) {
          response = await articleApi.getByCategory(categoryId, pageNum, size);
        } else if (tagId) {
          response = await articleApi.getByTag(tagId, pageNum, size);
        } else {
          response = await articleApi.getList(pageNum, size);
        }

        const data = response.data as PageResponse<Article>;

        setArticles(prev => (append ? [...prev, ...data.content] : data.content));
        setPageInfo({
          totalPages: data.totalPages,
          currentPage: data.number,
          hasMore: !data.last,
        });
      } catch (err) {
        setError('获取文章失败');
        console.error('Failed to fetch articles:', err);
      } finally {
        setIsLoading(false);
      }
    },
    [categoryId, tagId, keyword, size]
  );

  useEffect(() => {
    fetchArticles(page);
  }, [fetchArticles, page]);

  const refresh = () => fetchArticles(0);
  const loadMore = () => {
    if (pageInfo.hasMore && !isLoading) {
      fetchArticles(pageInfo.currentPage + 1, true);
    }
  };

  return {
    articles,
    isLoading,
    error,
    totalPages: pageInfo.totalPages,
    currentPage: pageInfo.currentPage,
    hasMore: pageInfo.hasMore,
    refresh,
    loadMore,
  };
};

// 获取单篇文章
export const useArticle = (id: number) => {
  const [article, setArticle] = useState<Article | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchArticle = async () => {
      setIsLoading(true);
      setError(null);

      try {
        const response = await articleApi.getById(id);
        setArticle(response.data);
      } catch (err) {
        setError('获取文章失败');
        console.error('Failed to fetch article:', err);
      } finally {
        setIsLoading(false);
      }
    };

    if (id) {
      fetchArticle();
    }
  }, [id]);

  return { article, isLoading, error };
};

// 获取精选文章
export const useFeaturedArticles = () => {
  const [articles, setArticles] = useState<Article[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchFeatured = async () => {
      try {
        const response = await articleApi.getFeatured();
        setArticles(response.data);
      } catch (err) {
        console.error('Failed to fetch featured articles:', err);
      } finally {
        setIsLoading(false);
      }
    };

    fetchFeatured();
  }, []);

  return { articles, isLoading };
};
