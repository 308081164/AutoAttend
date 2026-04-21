import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:webview_flutter/webview_flutter.dart';

/// 默认加载的生产 Web 基址；CI 传入 --dart-define=APP_BASE_URL=...
const String kDefaultBaseUrl = String.fromEnvironment(
  'APP_BASE_URL',
  defaultValue: 'https://example.com',
);

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const LiubangShellApp());
}

class LiubangShellApp extends StatelessWidget {
  const LiubangShellApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '流帮 Project',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF1456F0)),
        useMaterial3: true,
      ),
      debugShowCheckedModeBanner: false,
      home: const SplashPage(),
    );
  }
}

// ============================================================
//  启动页：品牌展示，2秒后自动跳转到主页面
// ============================================================

class SplashPage extends StatefulWidget {
  const SplashPage({super.key});

  @override
  State<SplashPage> createState() => _SplashPageState();
}

class _SplashPageState extends State<SplashPage> with SingleTickerProviderStateMixin {
  late final AnimationController _controller;
  late final Animation<double> _fadeAnimation;
  late final Animation<Offset> _slideAnimation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1200),
    );

    _fadeAnimation = CurvedAnimation(
      parent: _controller,
      curve: Curves.easeIn,
    );

    _slideAnimation = Tween<Offset>(
      begin: const Offset(0, 0.15),
      end: Offset.zero,
    ).animate(CurvedAnimation(
      parent: _controller,
      curve: Curves.easeOutCubic,
    ));

    _controller.forward();

    // 2秒后跳转到主页面
    Future.delayed(const Duration(milliseconds: 2500), () {
      if (mounted) {
        Navigator.of(context).pushReplacement(
          PageRouteBuilder(
            pageBuilder: (context, animation, secondaryAnimation) =>
                const ShellHomePage(),
            transitionsBuilder: (context, animation, secondaryAnimation, child) {
              return FadeTransition(opacity: animation, child: child);
            },
            transitionDuration: const Duration(milliseconds: 400),
          ),
        );
      }
    });
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    // 强制竖屏（手机端启动页竖屏展示）
    SystemChrome.setPreferredOrientations([
      DeviceOrientation.portraitUp,
    ]).catchError((_) {});

    final size = MediaQuery.of(context).size;
    final isTablet = size.shortestSide >= 600;

    return Scaffold(
      backgroundColor: const Color(0xFF1456F0),
      body: SafeArea(
        child: Center(
          child: Padding(
            padding: EdgeInsets.symmetric(
              horizontal: isTablet ? 80 : 40,
              vertical: isTablet ? 60 : 40,
            ),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                // Logo
                FadeTransition(
                  opacity: _fadeAnimation,
                  child: SlideTransition(
                    position: _slideAnimation,
                    child: Container(
                      width: isTablet ? 120 : 88,
                      height: isTablet ? 120 : 88,
                      decoration: BoxDecoration(
                        color: Colors.white.withValues(alpha: 0.15),
                        borderRadius: BorderRadius.circular(24),
                      ),
                      child: const Icon(
                        Icons.auto_awesome,
                        size: 48,
                        color: Colors.white,
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 32),

                // 品牌名称
                FadeTransition(
                  opacity: _fadeAnimation,
                  child: SlideTransition(
                    position: _slideAnimation,
                    child: Text(
                      '流帮 Project',
                      style: TextStyle(
                        color: Colors.white,
                        fontSize: isTablet ? 36 : 28,
                        fontWeight: FontWeight.w700,
                        letterSpacing: 2,
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 16),

                // Slogan
                FadeTransition(
                  opacity: _fadeAnimation,
                  child: SlideTransition(
                    position: _slideAnimation,
                    child: Text(
                      '软件流程帮你搞定！',
                      style: TextStyle(
                        color: Colors.white.withValues(alpha: 0.85),
                        fontSize: isTablet ? 20 : 16,
                        fontWeight: FontWeight.w400,
                        letterSpacing: 1,
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 48),

                // 加载指示器
                FadeTransition(
                  opacity: _fadeAnimation,
                  child: SizedBox(
                    width: 24,
                    height: 24,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      color: Colors.white.withValues(alpha: 0.6),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

// ============================================================
//  主页面：WebView 加载 Web 控制台
// ============================================================

class ShellHomePage extends StatefulWidget {
  const ShellHomePage({super.key});

  @override
  State<ShellHomePage> createState() => _ShellHomePageState();
}

class _ShellHomePageState extends State<ShellHomePage> {
  WebViewController? _controller;
  bool _loading = true;
  late final Uri _startUri;
  late final String _sameHost;

  @override
  void initState() {
    super.initState();
    // 进入主页面后，平板端切换为横屏
    _setOrientationByDevice();
    _init();
  }

  void _setOrientationByDevice() {
    final size = MediaQuery.of(context).size;
    final isTablet = size.shortestSide >= 600;
    if (isTablet) {
      SystemChrome.setPreferredOrientations([
        DeviceOrientation.landscapeLeft,
        DeviceOrientation.landscapeRight,
      ]).catchError((_) {});
    } else {
      SystemChrome.setPreferredOrientations([
        DeviceOrientation.portraitUp,
      ]).catchError((_) {});
    }
  }

  Future<void> _init() async {
    final info = await PackageInfo.fromPlatform();
    final ver = info.version;
    final build = info.buildNumber;
    String plat = 'unknown';
    if (Platform.isAndroid) plat = 'android';
    if (Platform.isIOS) plat = 'ios';
    if (Platform.isWindows) plat = 'windows';
    if (Platform.isLinux) plat = 'linux';
    if (Platform.isMacOS) plat = 'macos';

    final parsed = Uri.parse(kDefaultBaseUrl.trim());
    final qp = Map<String, String>.from(parsed.queryParameters);
    qp['clientVersion'] = ver;
    qp['clientBuild'] = build;
    qp['clientPlatform'] = plat;
    final uri = parsed.replace(queryParameters: qp);
    _startUri = uri;
    _sameHost = parsed.host.toLowerCase();

    final c = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setNavigationDelegate(
        NavigationDelegate(
          onNavigationRequest: (request) {
            final u = Uri.tryParse(request.url);
            if (u != null &&
                (u.scheme == 'http' || u.scheme == 'https') &&
                u.host.isNotEmpty &&
                u.host.toLowerCase() != _sameHost) {
              launchUrl(u, mode: LaunchMode.externalApplication);
              return NavigationDecision.prevent;
            }
            return NavigationDecision.navigate;
          },
          onPageFinished: (_) {
            if (mounted) setState(() => _loading = false);
          },
        ),
      )
      ..loadRequest(uri);

    if (mounted) {
      setState(() {
        _controller = c;
      });
    }
  }

  Future<void> _reload() async {
    if (_controller == null) return;
    setState(() => _loading = true);
    await _controller!.loadRequest(_startUri);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('流帮 Project'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _reload,
            tooltip: '刷新',
          ),
        ],
      ),
      body: Stack(
        children: [
          if (_controller != null)
            WebViewWidget(controller: _controller!)
          else
            const Center(child: CircularProgressIndicator()),
          if (_loading && _controller != null)
            const Align(
              alignment: Alignment.topCenter,
              child: LinearProgressIndicator(minHeight: 2),
            ),
        ],
      ),
    );
  }
}
