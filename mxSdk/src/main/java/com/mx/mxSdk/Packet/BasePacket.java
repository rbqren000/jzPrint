package com.mx.mxSdk.Packet;

import com.mx.mxSdk.TransportProtocol;
import com.mx.mxSdk.Utils.Arrays;
import com.mx.mxSdk.Utils.RBQLog;

import java.nio.charset.StandardCharsets;

public class BasePacket {

    public boolean startSendingData = false;

    public boolean isStartSendingData() {
        return startSendingData;
    }
    public void setStartSendingData(boolean startSendingData) {
        this.startSendingData = startSendingData;
    }

    public void clear(){
        this.startSendingData = false;
    }

    /*
    public boolean isRequest(byte[] data) {

        if (data == null) {
            return false;
        }

        if (!startSendingData) {
            return false;
        }

        String jsonStr = new String(data, StandardCharsets.UTF_8).trim();

        if (jsonStr.equalsIgnoreCase("c")) {
            return true;
        }
        if (data.length == 1 && ((data[0] & 0xFF) == TransportProtocol.C)) {
            return true;
        } else {
            boolean isHead = true;
            for (byte datum : data) {
                if ((datum & 0xFF) != TransportProtocol.C) {
                    isHead = false;
                    break;
                }
            }
            return isHead;
        }
    }
*/
    public boolean isRequest(byte[] data) {
        // 如果数据为空或者没有开始发送数据，直接返回 false
        if (data == null || !startSendingData) {
            return false;
        }

        // 检查数据长度为 1 的情况
        if (data.length == 1) {
            return (data[0] & 0xFF) == TransportProtocol.C;
        }

        // 尝试将数据转换为字符串并去掉前后空格
        String jsonStr = new String(data, StandardCharsets.UTF_8).trim();
        // 如果字符串等于 "n"（忽略大小写），返回 true
        if (jsonStr.equalsIgnoreCase("n")) {
            return true;
        }

        RBQLog.i("【粘包】-> 请求数据N; 16进制字符串:" + Arrays.bytesToHexString(data, ",") + "; 字符串:" + jsonStr);

        boolean hasN = false;
        boolean hasNAK = false;
        boolean hasEOT = false;
        for (byte _data : data) {
            int value = _data & 0xFF;
            if (value == TransportProtocol.C) {
                hasN = true;
            } else if (value == TransportProtocol.NAK) {
                hasNAK = true;
            } else if (value == TransportProtocol.EOT) {
                hasEOT = true;
            }
            // 提前终止循环，如果发现NAK或EOT
            if (hasNAK || hasEOT) {
                return false;
            }
        }
        return hasN;
    }

    public boolean isNAK(byte[] data) {
        // 如果数据为空或者没有开始发送数据，直接返回 false
        if (data == null || !startSendingData) {
            return false;
        }

        // 检查数据长度为 1 的情况
        if (data.length == 1) {
            return (data[0] & 0xFF) == TransportProtocol.NAK;
        }

        // 尝试将数据转换为字符串并去掉前后空格
        String jsonStr = new String(data, StandardCharsets.UTF_8).trim();
        // 如果字符串等于 "r"（忽略大小写），返回 true
        if (jsonStr.equalsIgnoreCase("r")) {
            return true;
        }

        RBQLog.i("【粘包】-> NAK; 16进制字符串:" + Arrays.bytesToHexString(data, ",") + "; 字符串:" + jsonStr);

        boolean hasNAK = false;
        boolean hasEOT = false;
        for (byte _data : data) {
            int value = _data & 0xFF;
            if (value == TransportProtocol.NAK) {
                hasNAK = true;
            } else if (value == TransportProtocol.EOT) {
                hasEOT = true;
                break;  // 提前终止循环，如果发现EOT
            }
        }
        return hasNAK && !hasEOT;
    }

    public boolean isEOT(byte[] data) {
        // 如果数据为空或者没有开始发送数据，直接返回 false
        if (data == null || !startSendingData) {
            return false;
        }
        // 检查数据长度为 1 的情况
        if (data.length == 1) {
            return (data[0] & 0xFF) == TransportProtocol.EOT;
        }
        // 尝试将数据转换为字符串并去掉前后空格
        String jsonStr = new String(data, StandardCharsets.UTF_8).trim();
        // 如果字符串等于 "c"（忽略大小写），返回 true
        if (jsonStr.equalsIgnoreCase("d")) {
            return true;
        }
        RBQLog.i("【粘包】-> EOT; 16进制字符串:"+ Arrays.bytesToHexString(data,",")+"; 字符串:"+jsonStr);
        // 检查是否包含EOT TransportProtocol.EOT
        for (byte _data : data) {
            if ((_data & 0xFF) == TransportProtocol.EOT) {
                return true;
            }
        }
        return false;
    }

}
