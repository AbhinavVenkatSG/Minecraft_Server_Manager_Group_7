const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:9000/api').replace(/\/$/, '');

class ApiService {
  async request(endpoint, options = {}) {
    const url = `${API_BASE_URL}${endpoint}`;
    const config = {
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
      ...options,
    };

    if (config.body && typeof config.body === 'object') {
      config.body = JSON.stringify(config.body);
    }

    try {
      const response = await fetch(url, config);
      const text = await response.text();
      
      try {
        return { data: JSON.parse(text), status: response.status };
      } catch {
        return { data: text, status: response.status };
      }
    } catch (error) {
      throw new Error(`API request failed: ${error.message}`);
    }
  }

  async get(endpoint) {
    return this.request(endpoint, { method: 'GET' });
  }

  async post(endpoint, body) {
    return this.request(endpoint, { method: 'POST', body });
  }

  async register(username, password) {
    return this.post('/auth/register', { username, password });
  }

  async login(username, password) {
    return this.post('/auth/login', { username, password });
  }

  async getServers() {
    return this.get('/servers');
  }

  async getServer(id) {
    return this.get(`/servers/${id}`);
  }

  async createServer(serverData) {
    return this.post('/servers', serverData);
  }

  async startServer(id) {
    return this.post(`/servers/${id}/start`);
  }

  async stopServer(id) {
    return this.post(`/servers/${id}/stop`);
  }

  async restartServer(id) {
    return this.post(`/servers/${id}/restart`);
  }

  async getTelemetry(id) {
    return this.get(`/servers/${id}/telemetry`);
  }

  async getConsoleLog(id) {
    return this.get(`/servers/${id}/console-log`);
  }

  async sendConsoleCommand(id, command) {
    return this.post(`/servers/${id}/console`, { command });
  }

  async getBackups(id) {
    return this.get(`/servers/${id}/backups`);
  }

  async createBackup(id) {
    return this.post(`/servers/${id}/backups`);
  }

  async getServerProperties(id) {
    return this.get(`/servers/${id}/files/server-properties`);
  }

  async updateServerProperties(id, content) {
    return this.post(`/servers/${id}/files/server-properties`, { content });
  }

  async getWhitelist(id) {
    return this.get(`/servers/${id}/files/whitelist`);
  }

  async updateWhitelist(id, content) {
    return this.post(`/servers/${id}/files/whitelist`, { content });
  }

  async getStartParameters(id) {
    return this.get(`/servers/${id}/start-parameters`);
  }

  async updateStartParameters(id, content) {
    return this.post(`/servers/${id}/start-parameters`, { content });
  }
}

export const api = new ApiService();
export default api;
