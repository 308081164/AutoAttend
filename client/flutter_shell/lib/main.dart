import 'dart:io';

import 'package:flutter/material.dart';
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
      home: const ShellHomePage(),
    );
  }
}

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
    _init();
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
