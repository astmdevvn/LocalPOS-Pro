# LocalPOS Pro

**Scan. Sell. Print.**

LocalPOS Pro là ứng dụng bán hàng Android dành cho cửa hàng nhỏ, shop mini, quầy kiosk, pop-up store và người bán hàng cần một công cụ POS đơn giản ngay trên điện thoại.

Ứng dụng được thiết kế theo hướng **offline-first**: các dữ liệu quan trọng như sản phẩm, tồn kho, giao dịch và công nợ được lưu trực tiếp trên thiết bị. Người dùng không cần tạo tài khoản để bắt đầu sử dụng và ứng dụng không tự động tải dữ liệu bán hàng lên máy chủ.

## Điểm nổi bật

- Bán hàng nhanh ngay trên điện thoại.
- Tìm sản phẩm theo tên hoặc mã vạch.
- Quét QR/barcode bằng CameraX và ML Kit.
- Quản lý giỏ hàng và thanh toán.
- Ghi nợ khách hàng với chế độ Pay Later.
- Theo dõi tồn kho và cảnh báo sản phẩm sắp hết.
- Thêm ảnh để nhận biết sản phẩm dễ dàng hơn.
- In hóa đơn bằng máy in Bluetooth ESC/POS.
- Báo cáo doanh thu theo ngày, 7 ngày và 30 ngày.
- Xuất dữ liệu dạng CSV để mở bằng Excel.
- Xuất báo cáo bán hàng dạng PDF.
- Backup và restore dữ liệu bằng tệp JSON.
- Có thể lưu hoặc mở bản backup thông qua trình chọn tệp của Android, bao gồm Google Drive nếu thiết bị đã cấu hình Drive.
- Import sản phẩm từ WooCommerce REST API.

## Quyền riêng tư và dữ liệu

LocalPOS Pro ưu tiên quyền kiểm soát dữ liệu của người dùng:

- Cơ sở dữ liệu được lưu cục bộ bằng Room/SQLite.
- Không yêu cầu tài khoản LocalPOS để sử dụng các chức năng offline.
- Không tự động đồng bộ sản phẩm, giao dịch hoặc công nợ lên máy chủ.
- Backup chỉ được tạo khi người dùng chủ động yêu cầu.
- Người dùng tự chọn nơi lưu và tệp cần khôi phục.
- Thông tin kết nối WooCommerce chỉ được sử dụng để thực hiện yêu cầu import do người dùng khởi tạo.
- Khi xoá sản phẩm, các giao dịch bán hàng đã ghi nhận trước đó vẫn được giữ lại.

Người dùng nên backup dữ liệu định kỳ, đặc biệt trước khi đổi điện thoại, xoá ứng dụng hoặc khôi phục cài đặt gốc.

## Chức năng bán hàng

### Sản phẩm và tồn kho

- Thêm sản phẩm với tên, mã vạch, giá bán và số lượng tồn.
- Chọn ảnh sản phẩm từ thư viện của thiết bị.
- Tìm kiếm nhanh theo tên hoặc mã vạch.
- Tự động giảm tồn kho sau khi hoàn tất thanh toán.
- Hiển thị trạng thái tồn kho thấp.
- Xoá sản phẩm không còn kinh doanh với bước xác nhận để tránh thao tác nhầm.

### Quét mã

Ứng dụng sử dụng CameraX để hiển thị camera và ML Kit Barcode Scanning để nhận dạng mã. Khi tìm thấy mã phù hợp, sản phẩm có thể được đưa trực tiếp vào giỏ hàng.

### Thanh toán và công nợ

- Thanh toán tiền mặt.
- Ghi nợ khách hàng bằng Pay Later.
- Theo dõi danh sách khoản nợ chưa thu.
- Đánh dấu khoản nợ đã thu khi khách hoàn tất thanh toán.

### In hóa đơn Bluetooth

LocalPOS Pro hỗ trợ kết nối với máy in Bluetooth dùng giao thức ESC/POS:

1. Ghép đôi máy in trong phần cài đặt Bluetooth của Android.
2. Chọn máy in trong LocalPOS Pro.
3. Hoàn tất thanh toán để in hóa đơn.

Khả năng tương thích thực tế có thể khác nhau tùy firmware, khổ giấy và tập lệnh ESC/POS của từng máy in.

## Báo cáo và xuất dữ liệu

Màn hình báo cáo cung cấp:

- Doanh thu và số đơn hôm nay.
- Thống kê trong 7 ngày gần nhất.
- Thống kê trong 30 ngày gần nhất.
- Danh sách các giao dịch trong kỳ được chọn.

Dữ liệu có thể được xuất thành:

- **CSV:** tương thích với Excel và nhiều ứng dụng bảng tính.
- **PDF:** phù hợp để lưu trữ, gửi hoặc in báo cáo.
- **JSON:** dùng cho backup và restore dữ liệu LocalPOS Pro.

## WooCommerce

Ứng dụng có thể import sản phẩm thông qua WooCommerce REST API. Người dùng cần cung cấp:

- URL cửa hàng.
- Consumer key.
- Consumer secret.

Nên tạo bộ thông tin API chỉ có các quyền thật sự cần thiết và không chia sẻ thông tin này trong ảnh chụp, issue hoặc nội dung công khai.

## Công nghệ sử dụng

- Kotlin
- Jetpack Compose Material 3
- Room / SQLite
- Kotlin Coroutines và Flow
- CameraX
- Google ML Kit Barcode Scanning
- Android Bluetooth API
- ESC/POS
- Android Storage Access Framework
- Android `PdfDocument`
- WooCommerce REST API

## Yêu cầu

- Android 8.0 (API 26) trở lên.
- Camera để quét mã vạch.
- Bluetooth để sử dụng máy in ESC/POS.
- Kết nối Internet khi import WooCommerce hoặc làm việc với nhà cung cấp lưu trữ trực tuyến.
- Ứng dụng Google Drive hoặc nhà cung cấp tài liệu tương thích nếu muốn lưu backup lên dịch vụ đám mây qua trình chọn tệp Android.

## Quyền Android

Tùy tính năng được sử dụng, ứng dụng có thể yêu cầu:

- **Camera:** quét mã QR và barcode.
- **Bluetooth:** tìm và kết nối máy in đã ghép đôi.
- **Thông báo:** cảnh báo sản phẩm sắp hết hàng.
- **Internet:** kết nối WooCommerce và dịch vụ do người dùng lựa chọn.

Ứng dụng chỉ yêu cầu quyền tại thời điểm chức năng tương ứng cần sử dụng.

## Cài đặt cho nhà phát triển

1. Cài Android Studio và JDK 17.
2. Mở thư mục dự án bằng Android Studio.
3. Chờ Gradle đồng bộ các thư viện.
4. Chọn thiết bị Android hoặc máy ảo.
5. Chạy cấu hình `app`.

Thông số Android hiện tại:

- Minimum SDK: 26
- Target SDK: 35
- Compile SDK: 35

## Cấu trúc chính

```text
app/src/main/java/com/localpos/pro/
├── data/           # Room database, entity, DAO và repository
├── integration/    # In Bluetooth, xuất dữ liệu và thông báo
├── ui/             # Giao diện Compose và ViewModel
├── LocalPosApplication.kt
└── MainActivity.kt
```

## Lưu ý khi sử dụng

- Đây là phần mềm hỗ trợ vận hành bán hàng; người dùng cần tự kiểm tra dữ liệu trước khi dùng cho kế toán, thuế hoặc đối soát chính thức.
- Nên thử máy in bằng giao dịch mẫu trước khi đưa vào sử dụng thực tế.
- Không xoá ứng dụng hoặc dữ liệu ứng dụng trước khi tạo bản backup cần thiết.
- Không đưa Consumer secret, tệp backup chứa dữ liệu thật hoặc thông tin khách hàng vào issue công khai.

## Định hướng phát triển

- Hoàn thiện quản lý nhập hàng và lịch sử điều chỉnh tồn kho.
- Báo cáo theo khoảng thời gian tùy chọn.
- Mẫu hóa đơn tùy chỉnh theo cửa hàng.
- Cải thiện khả năng tương thích với nhiều máy in ESC/POS.
- Kiểm tra và xác nhận backup trước khi restore.
- Hỗ trợ đăng ký thiết bị cho bản phát hành miễn phí có quản lý.
- Bổ sung kiểm thử tự động cho cơ sở dữ liệu và các luồng bán hàng quan trọng.

## Thương hiệu

LocalPOS Pro được phát triển theo định hướng: phần mềm bán hàng nhỏ gọn, dễ triển khai và trao quyền kiểm soát dữ liệu cho chính người sử dụng.

**ASLDEV.VN — Building Apps That Matter.**

Copyright © 2026 ASLDEV.VN.

