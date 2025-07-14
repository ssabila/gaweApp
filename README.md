<div align="center">
  <img src="src/main/resources/public/gawe-logo.png" alt="Gawe Logo" width="300">
  <h1>gawe - Sistem Manajemen Karyawan</h1>
  <p>Aplikasi manajemen Sumber Daya Manusia (SDM) komprehensif berbasis desktop yang dibangun dengan JavaFX dan backend MySQL. Gawe menyediakan platform yang kuat untuk mengelola karyawan, melacak kinerja, dan menyederhanakan alur kerja HR melalui antarmuka yang bersih dan berbasis peran.</p>
</div>


## 📋 Tentang Proyek

Gawe adalah aplikasi client-server yang dirancang untuk memusatkan dan menyederhanakan operasi HR untuk bisnis skala kecil hingga menengah. Aplikasi ini memiliki dasbor yang berbeda untuk Karyawan, Supervisor, dan Manajer, masing-masing disesuaikan dengan fungsionalitas spesifik untuk memastikan pemisahan tugas yang jelas dan alur kerja yang efisien.

Aplikasi ini menangani segalanya mulai dari absensi harian dan pengajuan cuti hingga evaluasi kinerja bulanan dan perhitungan gaji, menjadikannya solusi terpadu untuk manajemen tenaga kerja.

### Fitur Utama

  * **Kontrol Akses Berbasis Peran:** Tiga peran pengguna yang berbeda (Karyawan, Supervisor, Manajer) dengan dasbor dan izin yang disesuaikan.
  * **Database Terpusat:** Semua data aplikasi disimpan dan dikelola dalam database MySQL yang andal.
  * **Absensi Real-time:** Karyawan dapat melakukan *clock in* dan *clock out*, dengan pelacakan keterlambatan otomatis.
  * **Profil Karyawan Komprehensif:** Mengelola detail pribadi, peran pekerjaan, dan metrik kinerja.
  * **Manajemen Kinerja:** Menetapkan dan melacak *Key Performance Indicators* (KPI) per divisi dan melakukan evaluasi karyawan bulanan.
  * **Sistem Manajemen Cuti:** Alur kerja lengkap untuk mengajukan, menyetujui, dan menolak permintaan cuti, dengan pelacakan saldo cuti otomatis.
  * **Alur Kerja Pelaporan:** Supervisor dapat mengirimkan laporan bulanan, yang kemudian dapat ditinjau, disetujui, atau ditolak oleh manajer dengan catatan.
  * **Gaji & Penggajian:** Perhitungan gaji bulanan otomatis berdasarkan gaji pokok, bonus kinerja, dan penalti.
  * **Penjadwalan Rapat:** Pengguna dapat menjadwalkan pertemuan dengan anggota tim atau seluruh perusahaan.

## 🚀 Fitur Berdasarkan Peran

Aplikasi ini menyediakan pengalaman unik untuk setiap peran pengguna, memastikan mereka memiliki alat yang mereka butuhkan untuk melakukan tugas mereka secara efektif.

### 👤 Karyawan

Dasbor karyawan berfokus pada data pribadi dan tindakan layanan mandiri.

  * **Dasbor Pribadi:** Melihat statistik pribadi seperti skor KPI, saldo cuti, dan peringkat keseluruhan.
  * **Absensi:** Melakukan *clock in* dan *clock out* untuk hari itu.
  * **Melihat Jadwal:** Memeriksa riwayat absensi pribadi dan jadwal pertemuan.
  * **Pengajuan Cuti:** Mengajukan permintaan cuti baru untuk berbagai alasan (Tahunan, Sakit, dll.) dan melihat status permintaan sebelumnya.
  * **Informasi Gaji:** Melihat rincian gaji bulanan, termasuk bonus dan penalti, serta riwayat data gaji.
  * **Manajemen Profil:** Mengedit nama dan kata sandi pribadi.

### 👥 Supervisor

Dasbor supervisor mencakup semua fitur karyawan, ditambah alat untuk manajemen dan pengawasan tim.

  * **Dasbor Tim:** Mendapatkan gambaran umum tentang ukuran tim, KPI rata-rata, dan jumlah karyawan yang berisiko.
  * **Manajemen Tim:** Melihat semua karyawan di dalam divisi mereka dan menambahkan karyawan baru.
  * **Evaluasi Bulanan:** Melakukan dan mengirimkan evaluasi kinerja bulanan yang terperinci untuk anggota tim, menilai mereka berdasarkan ketepatan waktu, kehadiran, dan produktivitas.
  * **Persetujuan Cuti:** Meninjau, menyetujui, atau menolak permintaan cuti yang diajukan oleh karyawan di divisi mereka.
  * **Pengiriman Laporan:** Menulis dan mengirimkan laporan kinerja dan aktivitas bulanan untuk divisi mereka untuk ditinjau oleh seorang manajer.
  * **Analitik Kinerja:** Menganalisis kinerja tim melalui tabel terperinci dan kartu ikhtisar.

### 👑 Manajer

Dasbor manajer menyediakan tingkat akses tertinggi dengan kemampuan pengawasan strategis.

  * **Dasbor Global:** Melihat statistik seluruh perusahaan, termasuk total karyawan, laporan yang tertunda, dan permintaan cuti yang tertunda.
  * **Manajemen KPI:** Menetapkan, melihat, dan memperbarui KPI bulanan untuk semua divisi perusahaan.
  * **Tinjauan Laporan:** Mengakses dan meninjau semua laporan supervisor yang dikirimkan, dengan opsi untuk menyetujui atau menolaknya dengan catatan umpan balik.
  * **Persetujuan Cuti:** Menyetujui atau menolak permintaan cuti dari semua karyawan, termasuk supervisor.
  * **Manajemen Gaji:** Melihat gambaran umum gaji untuk semua karyawan dan mengakses database riwayat gaji lengkap.
  * **Melihat Semua Riwayat:** Mengakses tabel riwayat terkonsolidasi untuk setiap fitur utama, termasuk KPI, laporan, evaluasi, cuti, rapat, dan absensi.

## 🛠️ Cara Penggunaan 

Ikuti instruksi ini untuk mendapatkan salinan lokal dan menjalankannya.

### Prasyarat

  * **JDK 17** atau yang lebih baru.
  * **Apache Maven**
  * **MySQL Server** (disarankan Versi 8.0)

### Instalasi & Pengaturan

1.  **Clone Repositori**

    ```sh
    git clone https://github.com/ssabila/gawe.git
    cd gawe/gaweApp-30288ae85c57b66a906058d9f1acd723484aa18e
    ```

2.  **Konfigurasi Database**

      * Mulai server MySQL Anda.
      * Buat database baru bernama `gawe_db`.
        ```sql
        CREATE DATABASE gawe_db;
        ```
      * Aplikasi dikonfigurasi untuk menggunakan nama pengguna `root` dengan kata sandi kosong secara default. Jika konfigurasi Anda berbeda, perbarui kredensial di `src/main/java/database/DatabaseConfig.java`.
      * Aplikasi akan secara otomatis membuat semua tabel yang diperlukan dan mengisinya dengan data sampel pada saat pertama kali dijalankan.

3.  **Build dan Jalankan Aplikasi**

      * Build proyek menggunakan Maven:
        ```sh
        mvn clean install
        ```
      * Jalankan aplikasi JavaFX:
        ```sh
        mvn javafx:run
        ```
      * Sebagai alternatif, Anda dapat menjalankan kelas `app.HelloApplication` langsung dari IDE Anda.

### Penggunaan

Setelah aplikasi berjalan, layar login akan muncul. Anda dapat menggunakan kredensial sampel berikut (didefinisikan dalam `MySQLDatabaseManager.java`) untuk masuk dengan peran yang berbeda:

| Peran | Nama Pengguna | Kata Sandi | Divisi |
| :--- | :--- | :--- | :--- |
| **Manajer** | `MNG001` | `password123` |  |
| **Supervisor**| `SUP001` | `password123` | HR |
| **Karyawan** | `EMP005` | `password123` | HR |

## 🏗️ Arsitektur

Aplikasi Gawe dibangun di atas **Arsitektur Client-Server**.

  * **Server (`GaweServer.java`):** Server Java multi-utas yang mendengarkan koneksi klien pada port 8080. Ia menggunakan `ExecutorService` untuk menangani banyak klien secara bersamaan.
  * **Klien (`HelloApplication.java`):** Klien desktop kaya fitur yang dibangun dengan JavaFX. Ini menyediakan antarmuka pengguna untuk semua peran.
  * **Komunikasi:** Klien dan server berkomunikasi melalui koneksi `Socket`. Permintaan dan respons diserialisasi ke dan dari format JSON menggunakan pustaka `Gson`.
  * **Penyimpanan Data (`MySQLDataStore.java`):** Semua operasi data ditangani melalui kelas penyimpanan data khusus yang berinteraksi dengan database MySQL menggunakan JDBC. Ini menggunakan *connection pool* **HikariCP** untuk koneksi database yang efisien dan andal.

## 🔧 Tumpukan Teknologi

  * **Bahasa:** Java 17
  * **Antarmuka Pengguna:** JavaFX 17.0.2
  * **Database:** MySQL 8.0
  * **Alat Build:** Apache Maven
  * **Database Pooling:** HikariCP
  * **Serialisasi JSON:** Google Gson

## 🗄️ Skema Database

Aplikasi ini secara otomatis membuat tabel berikut di database `gawe_db`:

```sql
-- Tabel karyawan utama dengan peran dan data pribadi
CREATE TABLE IF NOT EXISTS employees (...);

-- Indikator Kinerja Utama per divisi
CREATE TABLE IF NOT EXISTS kpi (...);

-- Laporan bulanan yang diserahkan oleh supervisor
CREATE TABLE IF NOT EXISTS reports (...);

-- Evaluasi umum karyawan
CREATE TABLE IF NOT EXISTS employee_evaluations (...);

-- Evaluasi karyawan bulanan terperinci oleh supervisor
CREATE TABLE IF NOT EXISTS monthly_evaluations (...);

-- Catatan kehadiran harian (clock-in/clock-out)
CREATE TABLE IF NOT EXISTS attendance (...);

-- Jadwal rapat
CREATE TABLE IF NOT EXISTS meetings (...);

-- Peserta untuk setiap rapat
CREATE TABLE IF NOT EXISTS meeting_participants (...);

-- Permintaan cuti dengan alur kerja persetujuan
CREATE TABLE IF NOT EXISTS leave_requests (...);

-- Catatan riwayat semua pembayaran gaji bulanan
CREATE TABLE IF NOT EXISTS salary_history (...);
```
