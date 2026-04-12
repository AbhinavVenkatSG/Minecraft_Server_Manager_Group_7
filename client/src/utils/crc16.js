const CRC16 = {
  calculate(data) {
    const bytes = typeof data === 'string' ? new TextEncoder().encode(data) : data;
    let crc = 0xffff;
    
    for (let i = 0; i < bytes.length; i++) {
      crc ^= bytes[i] << 8;
      for (let j = 0; j < 8; j++) {
        if (crc & 0x8000) {
          crc = (crc << 1) ^ 0x1021;
        } else {
          crc <<= 1;
        }
        crc &= 0xffff;
      }
    }
    
    return crc;
  },

  verify(data, expectedCrc) {
    const calculated = this.calculate(data);
    return calculated === expectedCrc;
  }
};

export default CRC16;