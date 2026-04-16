import 'package:flutter_test/flutter_test.dart';
import 'package:liubang_shell/main.dart';

void main() {
  testWidgets('Shell app builds', (WidgetTester tester) async {
    await tester.pumpWidget(const LiubangShellApp());
    expect(find.text('流帮 Project'), findsOneWidget);
  });
}
