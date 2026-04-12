class FileTransfer {
  constructor() {
    this.chunks = new Map();
    this.currentFileId = null;
    this.totalChunks = 0;
    this.receivedChunks = 0;
    this.onProgress = null;
    this.onComplete = null;
    this.onError = null;
  }

  setProgressCallback(callback) {
    this.onProgress = callback;
  }

  setCompleteCallback(callback) {
    this.onComplete = callback;
  }

  setErrorCallback(callback) {
    this.onError = callback;
  }

  addChunk(packet) {
    try {
      const parts = packet.payload.split('|');
      if (parts.length < 3) {
        this.onError?.('Invalid chunk format');
        return;
      }

      const chunkNumber = parseInt(parts[0], 10);
      const totalChunks = parseInt(parts[1], 10);
      const data = parts.slice(2).join('|');

      if (this.totalChunks === 0) {
        this.totalChunks = totalChunks;
      }

      if (chunkNumber > this.totalChunks || chunkNumber < 1) {
        this.onError?.(`Invalid chunk number: ${chunkNumber}`);
        return;
      }

      this.chunks.set(chunkNumber, data);
      this.receivedChunks = this.chunks.size;

      this.onProgress?.({
        current: this.receivedChunks,
        total: this.totalChunks,
        percentage: Math.round((this.receivedChunks / this.totalChunks) * 100)
      });

      if (this.receivedChunks === this.totalChunks) {
        this.complete();
      }
    } catch (error) {
      this.onError?.(`Error processing chunk: ${error.message}`);
    }
  }

  complete() {
    try {
      let combinedData = '';
      for (let i = 1; i <= this.totalChunks; i++) {
        const chunk = this.chunks.get(i);
        if (chunk === undefined) {
          this.onError?.(`Missing chunk: ${i}`);
          return;
        }
        combinedData += chunk;
      }

      const blob = new Blob([combinedData], { type: 'application/zip' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `backup_${new Date().toISOString().slice(0, 10)}.zip`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);

      this.onComplete?.(blob);
      this.reset();
    } catch (error) {
      this.onError?.(`Error completing transfer: ${error.message}`);
    }
  }

  reset() {
    this.chunks.clear();
    this.currentFileId = null;
    this.totalChunks = 0;
    this.receivedChunks = 0;
  }

  cancel() {
    this.reset();
  }

  getProgress() {
    if (this.totalChunks === 0) return null;
    return {
      current: this.receivedChunks,
      total: this.totalChunks,
      percentage: Math.round((this.receivedChunks / this.totalChunks) * 100)
    };
  }
}

export default FileTransfer;