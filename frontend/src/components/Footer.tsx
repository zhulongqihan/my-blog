import { Bot, Github, Mail, Terminal } from 'lucide-react';
import OnlineCount from './OnlineCount';
import OnlineAvatarStack from './OnlineAvatarStack';
import AchievementHub from './AchievementHub';
import { AI_AGENT_URL } from '../constants/externalLinks';
import './Footer.css';

const Footer = () => {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="footer">
      <div className="footer__terminal">
        <div className="footer__terminal-header">
          <div className="footer__terminal-dots">
            <span className="footer__terminal-dot footer__terminal-dot--red"></span>
            <span className="footer__terminal-dot footer__terminal-dot--yellow"></span>
            <span className="footer__terminal-dot footer__terminal-dot--green"></span>
          </div>
          <span className="footer__terminal-title">
            <Terminal size={12} /> terminal
          </span>
        </div>
        <div className="footer__terminal-body">
          <p className="footer__terminal-line">
            <span className="footer__terminal-prompt">$</span>
            <span className="footer__terminal-command">who am I</span>
          </p>
          <p className="footer__terminal-output">NJU CS Master · Full-stack Developer</p>
          <p className="footer__terminal-line">
            <span className="footer__terminal-prompt">$</span>
            <span className="footer__terminal-command">cat contact.txt</span>
          </p>
          <div className="footer__terminal-links">
            <a
              href="https://github.com/zhulongqihan"
              target="_blank"
              rel="noopener noreferrer"
              className="footer__link"
            >
              <Github size={16} strokeWidth={1.5} />
              <span>GitHub</span>
            </a>
            <a href="mailto:2511819891@qq.com" className="footer__link">
              <Mail size={16} strokeWidth={1.5} />
              <span>Email</span>
            </a>
            <a
              href={AI_AGENT_URL}
              target="_blank"
              rel="noopener noreferrer"
              className="footer__link"
            >
              <Bot size={16} strokeWidth={1.5} />
              <span>AI Agent</span>
            </a>
          </div>
          <p className="footer__terminal-line footer__terminal-line--cursor">
            <span className="footer__terminal-prompt">$</span>
            <span className="footer__terminal-cursor">_</span>
          </p>
        </div>
      </div>

      <div className="footer__bottom">
        <p className="footer__copyright">© {currentYear} My Blog. Crafted with care.</p>
        <p className="footer__built">
          Built with React + Spring Boot · <OnlineCount />
          <OnlineAvatarStack />
          <AchievementHub />
        </p>
      </div>
    </footer>
  );
};

export default Footer;
