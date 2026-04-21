import 'package:flutter_test/flutter_test.dart';
import 'package:liubang_shell/main.dart';

void main() {
  testWidgets('Shell app builds', (WidgetTester tester) async {
    await tester.pumpWidget(const LiubangShellApp());
    // 启动页有动画，需要 pump 多帧让 UI 渲染完成
    await tester.pump(const Duration(milliseconds: 100));
    await tester.pump(const Duration(milliseconds: 100));
    await tester.pump(const Duration(seconds: 2));
    expect(find.text('流帮 Project'), findsWidgets);
    expect(find.text('软件流程帮你搞定！'), findsOneWidget);
  });
}
