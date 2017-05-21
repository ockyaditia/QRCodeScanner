package com.example.ockyaditiasaputra.qrcodescanner;

/**
 * Created by Ocky Aditia Saputra on 15/11/2015.
 */
public class Connections {
    String host;

    public Connections() {
        host = "http://192.168.1.10:88";
    }

    public String con() {
        return "http://google.co.id";
    }

    public String getBarang() {
        return "" + host + "/kasir/get_barang.php";
    }

    public String insertTransaksi() {
        return "" + host + "/kasir/insert_transaksi.php";
    }

    public String getUser() {
        return "" + host + "/kasir/get_user.php";
    }

    public String insertUser() {
        return "" + host + "/kasir/insert_user.php";
    }

    public String updateBarang() {
        return "" + host + "/kasir/update_barang.php";
    }
}
