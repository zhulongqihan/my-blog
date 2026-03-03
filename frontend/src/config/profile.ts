export interface ProfileSkill {
  name: string;
  level: number;
}

export const PROFILE_STATS = {
  coffeeCount: 860,
  projectCount: 12,
};

export const PROFILE_SKILLS: ProfileSkill[] = [
  { name: 'Java 17 / Spring Boot 3 / Spring Security', level: 90 },
  { name: 'React 19 / TypeScript / Vite', level: 88 },
  { name: 'Vue 3 / Element Plus / Pinia', level: 80 },
  { name: 'MySQL / Redis / RabbitMQ', level: 84 },
  { name: 'Docker Compose / Nginx / Linux', level: 82 },
  { name: 'WebSocket(STOMP) / API 设计 / 缓存优化', level: 78 },
];
