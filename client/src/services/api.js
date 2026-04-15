export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:9000/api').replace(/\/$/, '');

/**
 * Thin wrapper around the HTTP API used by the dashboard.
 */
class ApiService {
  /**
   * Builds an absolute API URL for a relative endpoint.
   */
  buildUrl(endpoint) {
    return `${API_BASE_URL}${endpoint}`;
  }

  /**
   * Sends a request and returns a normalized `{ data, status }` shape.
   */
  async request(endpoint, options = {}) {
    const url = this.buildUrl(endpoint);
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

  /**
   * Sends a GET request.
   */
  async get(endpoint) {
    return this.request(endpoint, { method: 'GET' });
  }

  /**
   * Sends a POST request with a JSON body.
   */
  async post(endpoint, body) {
    return this.request(endpoint, { method: 'POST', body });
  }

  /**
   * Registers a user and returns the raw API response.
   */
  async register(username, password) {
    return this.post('/auth/register', { username, password });
  }

  /**
   * Logs a user in and returns the raw API response.
   */
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

  /**
   * Returns the direct download URL for a backup archive.
   */
  getBackupDownloadUrl(id) {
    return this.buildUrl(`/backups/${id}/download`);
  }

  /**
   * Downloads a backup archive and extracts a filename when present.
   */
  async downloadBackup(id) {
    const response = await fetch(this.getBackupDownloadUrl(id));
    if (!response.ok) {
      throw new Error(await this.parseErrorResponse(response, `Failed to download backup ${id}`));
    }

    return {
      blob: await response.blob(),
      filename: this.parseDownloadFilename(response.headers.get('Content-Disposition')),
    };
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

  /**
   * Extracts the filename from a Content-Disposition header.
   */
  parseDownloadFilename(contentDisposition) {
    const match = contentDisposition?.match(/filename="([^"]+)"/i);
    return match?.[1] || null;
  }

  /**
   * Reads the server's error response body, falling back to a caller-supplied message.
   */
  async parseErrorResponse(response, fallbackMessage) {
    const text = await response.text();
    if (!text) {
      return fallbackMessage;
    }

    try {
      return JSON.parse(text)?.message || fallbackMessage;
    } catch {
      return text;
    }
  }
}

export const api = new ApiService();
export default api;
