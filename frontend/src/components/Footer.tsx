import { Github, Twitter, Mail, Terminal } from 'lucide-react';
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
            <span className="footer__terminal-command">whoami</span>
          </p>
          <p className="footer__terminal-output">A passionate developer & blogger</p>
          <p className="footer__terminal-line">
            <span className="footer__terminal-prompt">$</span>
            <span className="footer__terminal-command">cat contact.txt</span>
          </p>
          <div className="footer__terminal-links">
            <a
              href="https://github.com"
              target="_blank"
              rel="noopener noreferrer"
              className="footer__link"
            >
              <Github size={16} strokeWidth={1.5} />
              <span>GitHub</span>
            </a>
            <a
              href="https://twitter.com"
              target="_blank"
              rel="noopener noreferrer"
              className="footer__link"
            >
              <Twitter size={16} strokeWidth={1.5} />
              <span>Twitter</span>
            </a>
            <a href="mailto:hello@myblog.com" className="footer__link">
              <Mail size={16} strokeWidth={1.5} />
              <span>Email</span>
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
        <p className="footer__built">Built with React + Spring Boot</p>
      </div>
    </footer>
  );
};

export default Footer;
