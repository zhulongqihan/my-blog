import { Bot, ExternalLink, Sparkles } from 'lucide-react';
import { Link } from 'react-router-dom';
import { AI_AGENT_NAME, AI_AGENT_SUMMARY, AI_AGENT_URL } from '../constants/externalLinks';
import type { Article } from '../types';
import './ProjectBridge.css';

interface ProjectBridgeProps {
  title: string;
  description: string;
  articleLinks?: Article[];
  compact?: boolean;
}

const DEFAULT_TOPICS = ['AI Agent', '稳定性分析', '工作流编排', '低配部署', '技术写作'];

const ProjectBridge = ({ title, description, articleLinks = [], compact = false }: ProjectBridgeProps) => {
  return (
    <section className={`project-bridge ${compact ? 'project-bridge--compact' : ''}`}>
      <div className="project-bridge__content">
        <div className="project-bridge__eyebrow">
          <Sparkles size={14} /> 最近在做
        </div>
        <h2 className="project-bridge__title">{title}</h2>
        <p className="project-bridge__text">{description}</p>

        <div className="project-bridge__card">
          <div className="project-bridge__card-head">
            <div>
              <p className="project-bridge__card-label">独立项目</p>
              <h3 className="project-bridge__card-title">{AI_AGENT_NAME}</h3>
            </div>
            <span className="project-bridge__card-icon">
              <Bot size={20} strokeWidth={1.6} />
            </span>
          </div>
          <p className="project-bridge__card-text">{AI_AGENT_SUMMARY}</p>
          <div className="project-bridge__topics">
            {DEFAULT_TOPICS.map(topic => (
              <span key={topic} className="project-bridge__topic">{topic}</span>
            ))}
          </div>
          <a
            href={AI_AGENT_URL}
            target="_blank"
            rel="noopener noreferrer"
            className="project-bridge__button"
          >
            <ExternalLink size={15} />
            看看这个项目
          </a>
        </div>

        {articleLinks.length > 0 && (
          <div className="project-bridge__articles">
            <p className="project-bridge__articles-label">如果你想先看文字版思考，可以从这些文章开始：</p>
            <div className="project-bridge__article-list">
              {articleLinks.map(article => (
                <Link key={article.id} to={`/article/${article.id}`} className="project-bridge__article-link">
                  {article.title}
                </Link>
              ))}
            </div>
          </div>
        )}
      </div>
    </section>
  );
};

export default ProjectBridge;