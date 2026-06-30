package com.melody.app.data.news

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.nio.ByteBuffer

/**
 * 自定义 DNS 解析器 + HTTP 连接工厂
 *
 * 解决设备系统 DNS 无法解析中文域名的问题。
 * 用 UDP 直接向公共 DNS 服务器（阿里 223.5.5.5）查询，
 * 获取 IP 后用 IP 连接 HTTPS（设置 Host 头）。
 */
object CustomDns {

    private val DNS_SERVERS = listOf("223.5.5.5", "114.114.114.114")
    private val dnsCache = mutableMapOf<String, String>()  // 域名 → IP 缓存

    /**
     * 创建使用自定义 DNS 的 HttpURLConnection
     * 自动解析域名为 IP，用 IP 连接，设置 Host 头
     */
    fun openConnection(urlString: String, timeoutMs: Int = 8000): HttpURLConnection {
        val url = URL(urlString)
        val host = url.host

        // 解析域名为 IP
        val ip = resolveCached(host)
        val connectionUrl = if (ip != null) {
            urlString.replace("://$host", "://$ip")
        } else {
            urlString
        }

        val conn = URL(connectionUrl).openConnection() as HttpURLConnection
        conn.setRequestProperty("Host", host)
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) Melody/1.0")
        conn.connectTimeout = timeoutMs
        conn.readTimeout = timeoutMs
        conn.instanceFollowRedirects = true
        return conn
    }

    /**
     * 带缓存的 DNS 解析
     */
    private fun resolveCached(host: String): String? {
        dnsCache[host]?.let { return it }
        val ip = resolve(host)
        if (ip != null) dnsCache[host] = ip
        return ip
    }

    /**
     * 用 UDP 向公共 DNS 查询域名 IP
     */
    private fun resolve(host: String): String? {
        for (server in DNS_SERVERS) {
            try {
                val ip = queryDns(host, server)
                if (ip != null) return ip
            } catch (_: Exception) { }
        }
        return null
    }

    private fun queryDns(host: String, dnsServer: String): String? {
        val socket = DatagramSocket()
        try {
            socket.soTimeout = 2000
            val query = buildQuery(host)
            val addr = InetAddress.getByName(dnsServer)
            socket.send(DatagramPacket(query, query.size, addr, 53))

            val buf = ByteArray(512)
            val resp = DatagramPacket(buf, buf.size)
            socket.receive(resp)
            return parseResponse(buf, resp.length)
        } finally {
            socket.close()
        }
    }

    private fun buildQuery(host: String): ByteArray {
        val bb = ByteBuffer.allocate(512)
        bb.putShort(0x1234)  // Transaction ID
        bb.putShort(0x0100)  // Flags
        bb.putShort(1)       // Questions
        bb.putShort(0); bb.putShort(0); bb.putShort(0)
        for (part in host.split(".")) {
            bb.put(part.length.toByte())
            bb.put(part.toByteArray())
        }
        bb.put(0)
        bb.putShort(1)  // Type A
        bb.putShort(1)  // Class IN
        return bb.array().sliceArray(0 until bb.position())
    }

    private fun parseResponse(data: ByteArray, len: Int): String? {
        if (len < 12) return null
        val bb = ByteBuffer.wrap(data, 0, len)
        bb.position(12)
        // Skip question
        while (bb.hasRemaining()) {
            val b = bb.get().toInt() and 0xFF
            if (b == 0) break
            if (b >= 0xC0) { bb.get(); break }
            bb.position(bb.position() + b)
        }
        if (bb.remaining() < 4) return null
        bb.position(bb.position() + 4)  // QTYPE + QCLASS

        // Parse answers
        while (bb.remaining() >= 12) {
            val nameByte = bb.get().toInt() and 0xFF
            if (nameByte >= 0xC0) bb.get()
            else {
                var l = nameByte
                while (l != 0 && bb.hasRemaining()) { bb.position(bb.position() + l); l = bb.get().toInt() and 0xFF }
            }
            if (bb.remaining() < 10) return null
            val type = bb.short.toInt() and 0xFFFF
            bb.short; bb.int
            val rdLen = bb.short.toInt() and 0xFFFF
            if (type == 1 && rdLen == 4) {
                return "${bb.get().toInt() and 0xFF}.${bb.get().toInt() and 0xFF}.${bb.get().toInt() and 0xFF}.${bb.get().toInt() and 0xFF}"
            }
            bb.position(bb.position() + rdLen)
        }
        return null
    }
}
