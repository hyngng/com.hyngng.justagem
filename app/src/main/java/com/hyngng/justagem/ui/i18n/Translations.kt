package com.hyngng.justagem.ui.i18n

/**
 * Multi-language translation registry covering Japanese, Chinese, Spanish, German, and French.
 *
 * English is used as the dictionary key to look up native translations.
 * Parameterized and regex-based patterns handle dynamic string templates.
 * If a translation is not found, it gracefully falls back to English.
 */
internal object Translations {

    private val JA = mapOf(
        " · applies to the next response" to " · 次の回答から適用",
        " · tools supported" to " · ツール対応",
        " · tools unavailable" to " · ツール非対応",
        "(no message)" to "(メッセージなし)",
        "A response is still in progress." to "回答がまだ進行中です。",
        "A short vibration marks each new response text chunk." to "新しい回答テキストを受信するたびに短い振動で通知します。",
        "About Justagem" to "Justagem について",
        "All vector extraction and photo analysis run on this device and are never sent to a server. That’s one of the best things about using an on-device model." to "すべてのベクトル抽出と写真分析はデバイス内でのみ実行され、サーバーに送信されることはありません。これがオンデバイスモデルの最大の利点です。",
        "Allow" to "許可",
        "An on-device model computes a vector for each photo, so it can take several minutes and use battery. " to "オンデバイスモデルが写真ごとにベクトルを計算するため、数分以上かかる場合がありバッテリーを消費します。 ",
        "Analyzing photos" to "写真を分析中",
        "Are you sure you want to delete this chat? This action cannot be undone." to "このチャットを削除してもよろしいですか？この操作は取り消せません。",
        "Ask me anything" to "何でも質問してください",
        "Attach an image" to "画像添付",
        "Attach image" to "画像を添付",
        "Available" to "利用可能",
        "Back" to "戻る",
        "Camera" to "カメラ",
        "Cancel" to "キャンセル",
        "Chats" to "チャット履歴",
        "Check again" to "再確認",
        "Check status again" to "ステータスを再確認",
        "Check the speech recognition connection." to "音声認識の接続を確認してください。",
        "Checking" to "確認中",
        "Checking the model status." to "モデルの状態を確認しています。",
        "Clear existing index data?" to "既存のインデックスデータを初期化しますか？",
        "Clear index" to "インデックスを初期化",
        "Clear photo index" to "写真インデックスを初期化",
        "Clear search" to "検索をクリア",
        "Close" to "閉じる",
        "Close full thoughts" to "思考の全文を閉じる",
        "Close language list" to "言語一覧を閉じる",
        "Close model list" to "モデル一覧を閉じる",
        "Close voice chat" to "音声対話を閉じる",
        "Co-Engineered by:" to "共同開発:",
        "Complete" to "完了",
        "Continuing with a normal response." to "通常の回答で処理を続行します。",
        "Conversations" to "会話一覧",
        "Copied" to "コピーしました",
        "Copy" to "コピー",
        "Could not build a response. Please try again." to "回答を作成できませんでした。もう一度お試しください。",
        "Could not fully clear the index." to "インデックスを完全に初期化できませんでした。",
        "Could not prepare speech recognition." to "音声認識エンジンを準備できませんでした。",
        "Could not read the saved index. Please retry." to "保存されたインデックスを読み込めませんでした。再試行してください。",
        "Could not save the conversation. Check available storage." to "会話を保存できませんでした。ストレージの空き容量を確認してください。",
        "Could not save the index. Please retry." to "インデックスを保存できませんでした。再試行してください。",
        "Could not start speech recognition." to "音声認識を開始できませんでした。",
        "Currently selected" to "現在選択中",
        "Defaults" to "デフォルト",
        "Delete" to "削除",
        "Delete all" to "すべて削除",
        "Delete chat" to "チャットを削除",
        "Dinner idea" to "夕食の献立",
        "Download" to "ダウンロード",
        "Download model" to "モデルをダウンロード",
        "Download needed" to "ダウンロードが必要",
        "Download required" to "ダウンロードが必要",
        "Downloading" to "ダウンロード中",
        "Downloading model" to "モデルをダウンロードしています",
        "E4B ready" to "E4B 準備完了",
        "Edit" to "編集",
        "Edit your message" to "メッセージを編集",
        "EmbeddingGemma 2 model could not be initialized (check AICore)." to "EmbeddingGemma 2モデルを初期化できませんでした(AICoreを確認してください)。",
        "Engineered by:" to "開発:",
        "Enroll in the AICore Developer Preview (get Gemini Nano v4 early)" to "AICore開発者プレビューに登録(Gemini Nano v4を先行取得)",
        "Exit voice mode" to "音声モードを終了",
        "Feedback" to "フィードバック",
        "Fetch this web page?" to "このウェブページを取得しますか？",
        "Follow system language" to "システム言語に従う",
        "Full thoughts" to "思考の全文",
        "Fun fact" to "今日の豆知識",
        "General" to "一般",
        "Generation stopped." to "生成を停止しました。",
        "HTTPS request failed" to "HTTPSリクエストに失敗しました",
        "Haptics during model responses" to "モデル回答時の触覚フィードバック",
        "I couldn't finish the combined answer, so here are the per-photo analyses." to "全体の回答を完了できなかったため、写真ごとの分析結果を表示します。",
        "I didn't catch that. Please say it again." to "聞き取れませんでした。もう一度お話しください。",
        "I didn't catch that. Please try again." to "聞き取れませんでした。もう一度お試しください。",
        "Idea" to "アイデア",
        "Idle" to "待機中",
        "Image analysis" to "画像分析",
        "Index" to "インデックス作成",
        "Indexed photos" to "インデックス済みの写真",
        "Indexing failed. The saved index was kept." to "インデックス作成に失敗しました。保存済みのインデックスは維持されました。",
        "Indexing photos" to "写真をインデックス中",
        "Indexing was interrupted." to "インデックス作成が中断されました。",
        "Just a moment…" to "少々お待ちください…",
        "Language" to "言語",
        "Lightweight model instead of E4B" to "E4Bの代わりに軽量モデルを使用",
        "Listening" to "聞き取り中",
        "Listening…" to "聞き取り中…",
        "Loading photo index" to "写真インデックスを読み込み中",
        "Loads the model onto the accelerator in advance so the first reply starts sooner. May use slightly more battery." to "アプリ起動時にモデルをアクセラレータに事前ロードして、最初の回答開始を早めます。バッテリーをわずかに多く消費する場合があります。",
        "Max output tokens (128-4096)" to "最大出力トークン (128-4096)",
        "Microphone permission is required." to "マイクの許可が必要です。",
        "Model" to "モデル",
        "Model needs setup" to "モデルの準備が必要です",
        "Model settings" to "モデル設定",
        "Model unavailable" to "モデルを利用できません",
        "Needs attention" to "確認が必要",
        "Needs download" to "ダウンロードが必要",
        "New chat" to "新しいチャット",
        "No chats yet" to "会話履歴はありません",
        "No matching chats" to "一致する会話がありません",
        "No photo permission" to "写真へのアクセス権限がありません",
        "No photos are indexed on this device. Create a photo index in generation settings first." to "この端末にはインデックス済みの写真がありません。まず生成設定で写真インデックスを作成してください。",
        "No response" to "応答なし",
        "No search results" to "検索結果はありません",
        "No speech was heard." to "音声が聞こえませんでした。",
        "No thought text" to "思考テキストなし",
        "Notice" to "お知らせ",
        "Notification permission" to "通知権限",
        "One moment" to "少々お待ちください",
        "Open language list" to "言語一覧を開く",
        "Open model list" to "モデル一覧を開く",
        "Open source licenses" to "オープンソースライセンス",
        "Paused" to "一時停止中",
        "Photo" to "写真",
        "Photo index cleared." to "写真インデックスを初期化しました。",
        "Photo indexing" to "写真のインデックス作成",
        "Photo indexing is complete." to "写真のインデックス作成が完了しました。",
        "Photos" to "写真",
        "Pins the app display language." to "アプリの表示言語を固定します。",
        "Plan a weekend trip for me." to "週末の旅行計画を立てて。",
        "Play sound effects during model responses" to "モデル回答時に効果音を再生",
        "Plays a gentle tap sound on new content chunks and tool calls." to "新しい回答チャンクやツール呼び出し時に優しいタップ音を鳴らします。",
        "Plays a sound as each new response text chunk arrives." to "回答テキストが新しく到着するたびに効果音を再生します。",
        "Pre-warm model (faster first reply)" to "モデルを事前ウォームアップ(初回回答を高速化)",
        "Prepares the model when the screen turns on to speed up initial generation." to "画面点灯時にモデルを準備し、初回生成を高速化します。",
        "Prompt API requirements and device support" to "Prompt APIの要件と対応端末",
        "Ready" to "準備完了",
        "Ready on device" to "デバイス上で準備完了",
        "Ready with a lightweight model" to "軽量モデルで準備完了",
        "Rename" to "名前を変更",
        "Rename chat" to "チャット名を変更",
        "Requires photo access to search device photos." to "端末内の写真を検索するには写真アクセス権限が必要です。",
        "Reset all settings to default values?" to "すべての設定を初期値に戻しますか？",
        "Reset to defaults" to "デフォルトに戻す",
        "Retry" to "再試行",
        "Run this tool?" to "このツールを実行しますか？",
        "Save" to "保存",
        "Save edit" to "編集を完了",
        "Search" to "検索",
        "Search chats" to "チャット検索",
        "See also" to "関連情報",
        " See the code sample on screen. " to " コード例は画面でご確認ください。 ",
        "Send" to "送信",
        "Send a message" to "メッセージを送信",
        "Set up the Gemma 4 model first." to "まずGemma 4モデルを準備してください。",
        "Settings" to "設定",
        "Speak" to "話す",
        "Speak whenever you're ready" to "いつでもお話しください",
        "Speaking" to "発話中",
        "Speaking…" to "話しています…",
        "Speech recognition error" to "音声認識エラー",
        "Speech recognition is busy. Please try again soon." to "音声認識が使用中です。しばらくしてから再度お試しください。",
        "Speech recognition is unavailable on this device." to "このデバイスでは音声認識を利用できません。",
        "Speech recognition is unavailable." to "音声認識を利用できません。",
        "Start" to "開始",
        "Start a new chat" to "新しいチャットを開始",
        "Start editing" to "編集を開始",
        "Start indexing" to "インデックス作成を開始",
        "Start indexing photos?" to "写真のインデックス作成を開始しますか？",
        "Start photo indexing" to "写真のインデックス作成を開始",
        "Status unknown" to "状態確認が必要",
        "Stop" to "停止",
        "Stop photo indexing" to "写真のインデックス作成を停止",
        "Stop response" to "回答を停止",
        "Stopped" to "停止済み",
        "Stopping" to "中止中",
        "Stopping partway keeps the photos already indexed and resumes from there next time." to "途中で停止してもインデックス済みの写真は保持され、次回そこから再開されます。",
        "Stopping photo indexing" to "写真インデックス作成の中止中",
        "Suggest an easy and delicious dinner I can make." to "手軽でおいしく作れる夕食のメニューをおすすめして。",
        "Suggest one fun idea I could try today." to "今日試せる楽しいアイデアをひとつ提案して。",
        "Supported device list" to "対応端末一覧",
        "System instructions and pre-prompt" to "システム指示と事前プロンプト",
        "Tap again to speak." to "もう一度タップして話してください。",
        "Tap the microphone to start" to "マイクをタップして開始",
        "Tap to cancel" to "タップしてキャンセル",
        "Tap to finish" to "タップして終了",
        "Tap to speak" to "タップして話す",
        "Tap to stop" to "タップして停止",
        "Tell me one interesting and useful science or everyday fact." to "面白くて役に立つ科学または日常の豆知識をひとつ教えて。",
        "Temperature (0.00-1.00)" to "温度 (0.00-1.00)",
        "The index was cleared." to "インデックスが初期化されました。",
        "The model could not be prepared" to "モデルを準備できませんでした",
        "The model is answering" to "モデルが回答中です",
        "The model is downloading." to "モデルをダウンロードしています。",
        "The model is unavailable on this device" to "この端末ではモデルを利用できません",
        "The model's thoughts appear here." to "モデルの思考過程がここに表示されます。",
        "The on-device model is unavailable on this device." to "この端末ではオンデバイスモデルを利用できません。",
        "The response was empty. Please try again." to "回答が空でした。もう一度お試しください。",
        "The user cancelled the run." to "ユーザーが実行をキャンセルしました。",
        "There is no message to send." to "送信するメッセージがありません。",
        "Thinking" to "思考中",
        "Thinking is skipped for voice replies · setting is kept" to "音声対話ではThinkingを一時的に省略します · 設定は維持されます",
        "Thinking mode" to "思考モード",
        "Thinking stopped" to "思考を停止しました",
        "Thinking unavailable" to "思考機能は利用できません",
        "This response has no thought text to show." to "この回答には表示する思考テキストがありません。",
        "This tool and argument combination was already tried once, so it was not retried." to "このツールと引数の組み合わせは既に一度試行されたため、再試行しませんでした。",
        "Thought complete" to "思考完了",
        "Tool execution timed out" to "ツールの実行がタイムアウトしました",
        "Top K (1-128)" to "Top K (1-128)",
        "Trip plan" to "旅行計画",
        "Unavailable" to "利用不可",
        "Unknown" to "不明",
        "Unknown date" to "日付不明",
        "User" to "ユーザー",
        "Using a tool" to "ツールを使用中",
        "Version" to "バージョン",
        "Vibrates on response start, completion, and tool calls." to "回答の開始、完了、ツール呼び出し時に振動します。",
        "Vibration feedback" to "振動フィードバック",
        "Voice chat" to "音声対話",
        "Voice chat needs microphone permission." to "音声対話にはマイク権限が必要です。",
        "Voice mode" to "音声モード",
        "Warm up model in advance" to "モデルを事前にウォームアップ",
        "Wi-Fi is recommended" to "Wi-Fi接続を推奨します",
        "Writing a reply" to "回答を作成中",
        "all" to "全体",
        "error: Photo search could not complete because of a local index or on-device embedding error. Do not repeat the same search; tell the user photo search failed." to "エラー: 写真インデックスまたはオンデバイス埋め込みの問題により写真検索を完了できませんでした。同じ検索を繰り返さず、検索失敗をユーザーに伝えてください。",
        "error: after cannot be later than before" to "エラー: afterはbeforeより後に設定できません",
        "error: after must be YYYY-MM-DD" to "エラー: afterはYYYY-MM-DD形式である必要があります",
        "error: before must be YYYY-MM-DD" to "エラー: beforeはYYYY-MM-DD形式である必要があります",
        "error: could not open an HTTPS connection" to "エラー: HTTPS接続を開けませんでした",
        "error: could not resolve the host" to "エラー: ホストを解決できませんでした",
        "error: local or private network addresses cannot be fetched" to "エラー: ローカルまたはプライベートネットワークのアドレスは取得できません",
        "error: only public HTTPS URLs are allowed" to "エラー: 公開HTTPS URLのみ許可されています",
        "error: query is required." to "エラー: 検索語句が必要です。",
        "error: query is required. e.g. search_conversations(query=\"coffee\", limit=5)" to "エラー: queryが必要です。例: search_conversations(query=\"コーヒー\", limit=5)",
        "error: query must be 200 characters or fewer" to "エラー: queryは200文字以内である必要があります",
        "error: redirect handling failed" to "エラー: リダイレクト処理に失敗しました",
        "error: the photo search query must be 120 characters or fewer." to "エラー: 写真検索語句は120文字以内で入力してください。",
        "error: url is required. e.g. web_fetch(url=\"https://example.com\")" to "エラー: urlが必要です。例: web_fetch(url=\"https://example.com\")",
        " link " to " リンク ",
    )

    private val ZH = mapOf(
        " · applies to the next response" to " · 从下一次回答起生效",
        " · tools supported" to " · 支持工具",
        " · tools unavailable" to " · 不支持工具",
        "(no message)" to "(无消息)",
        "A response is still in progress." to "回复仍在进行中。",
        "A short vibration marks each new response text chunk." to "每次收到新的回答文本片段时都会短振动提醒。",
        "About Justagem" to "关于 Justagem",
        "All vector extraction and photo analysis run on this device and are never sent to a server. That’s one of the best things about using an on-device model." to "所有向量提取与照片分析均仅在设备本地安全运行，绝不会上传至服务器。这也是端侧模型最吸引人的优势所在。",
        "Allow" to "允许",
        "An on-device model computes a vector for each photo, so it can take several minutes and use battery. " to "端侧模型需要为每张照片计算特征向量，因此可能需要数分钟并消耗一定电量。 ",
        "Analyzing photos" to "正在分析照片",
        "Are you sure you want to delete this chat? This action cannot be undone." to "确定要删除此对话吗？此操作无法撤销。",
        "Ask me anything" to "随意向我提问",
        "Attach an image" to "添加图片",
        "Attach image" to "添加图片",
        "Available" to "可用",
        "Back" to "返回",
        "Camera" to "相机",
        "Cancel" to "取消",
        "Chats" to "对话记录",
        "Check again" to "重新检查",
        "Check status again" to "重新检查状态",
        "Check the speech recognition connection." to "请检查语音识别连接。",
        "Checking" to "检查中",
        "Checking the model status." to "正在检查模型状态。",
        "Clear existing index data?" to "是否清除已存在的索引数据？",
        "Clear index" to "清除索引",
        "Clear photo index" to "清除照片索引",
        "Clear search" to "清除搜索",
        "Close" to "关闭",
        "Close full thoughts" to "关闭完整思考",
        "Close language list" to "关闭语言列表",
        "Close model list" to "关闭模型列表",
        "Close voice chat" to "关闭语音对话",
        "Co-Engineered by:" to "联合研发:",
        "Complete" to "已完成",
        "Continuing with a normal response." to "继续按普通回答生成。",
        "Conversations" to "对话列表",
        "Copied" to "已复制",
        "Copy" to "复制",
        "Could not build a response. Please try again." to "无法生成回答，请重试。",
        "Could not fully clear the index." to "无法完全清除索引。",
        "Could not prepare speech recognition." to "无法初始化语音识别引擎。",
        "Could not read the saved index. Please retry." to "无法读取已保存的索引，请重试。",
        "Could not save the conversation. Check available storage." to "无法保存对话，请检查可用存储空间。",
        "Could not save the index. Please retry." to "无法保存索引，请重试。",
        "Could not start speech recognition." to "无法启动语音识别。",
        "Currently selected" to "当前选择",
        "Defaults" to "默认值",
        "Delete" to "删除",
        "Delete all" to "全部删除",
        "Delete chat" to "删除对话",
        "Dinner idea" to "晚餐推荐",
        "Download" to "下载",
        "Download model" to "下载模型",
        "Download needed" to "需要下载",
        "Download required" to "需要下载",
        "Downloading" to "正在下载",
        "Downloading model" to "正在下载模型",
        "E4B ready" to "E4B 已就绪",
        "Edit" to "编辑",
        "Edit your message" to "编辑你的消息",
        "EmbeddingGemma 2 model could not be initialized (check AICore)." to "无法初始化 EmbeddingGemma 2 模型 (请检查 AICore)。",
        "Engineered by:" to "开发:",
        "Enroll in the AICore Developer Preview (get Gemini Nano v4 early)" to "加入 AICore 开发者预览计划 (抢先体验 Gemini Nano v4)",
        "Exit voice mode" to "退出语音模式",
        "Feedback" to "反馈",
        "Fetch this web page?" to "是否获取此网页？",
        "Follow system language" to "跟随系统语言",
        "Full thoughts" to "完整思考过程",
        "Fun fact" to "冷知识",
        "General" to "通用",
        "Generation stopped." to "已停止生成。",
        "HTTPS request failed" to "HTTPS 请求失败",
        "Haptics during model responses" to "模型回答时振动",
        "I couldn't finish the combined answer, so here are the per-photo analyses." to "未能完成综合回答，以下为单张照片的分析结果。",
        "I didn't catch that. Please say it again." to "没有听清，请再说一次。",
        "I didn't catch that. Please try again." to "没有听清，请重试。",
        "Idea" to "灵感建议",
        "Idle" to "空闲",
        "Image analysis" to "图像分析",
        "Index" to "创建索引",
        "Indexed photos" to "已索引照片",
        "Indexing failed. The saved index was kept." to "索引创建失败，已保留先前保存的索引。",
        "Indexing photos" to "正在索引照片",
        "Indexing was interrupted." to "索引已中断。",
        "Just a moment…" to "请稍候…",
        "Language" to "语言",
        "Lightweight model instead of E4B" to "使用轻量模型替代 E4B",
        "Listening" to "正在聆听",
        "Listening…" to "正在聆听…",
        "Loading photo index" to "正在加载照片索引",
        "Loads the model onto the accelerator in advance so the first reply starts sooner. May use slightly more battery." to "在应用启动时预先将模型载入加速芯片，以加快首次回答速度。可能会略微增加耗电量。",
        "Max output tokens (128-4096)" to "最大输出 Token 数 (128-4096)",
        "Microphone permission is required." to "需要麦克风权限。",
        "Model" to "模型",
        "Model needs setup" to "模型需要配置",
        "Model settings" to "模型设置",
        "Model unavailable" to "模型不可用",
        "Needs attention" to "需要关注",
        "Needs download" to "需要下载",
        "New chat" to "新对话",
        "No chats yet" to "暂无对话记录",
        "No matching chats" to "无匹配对话",
        "No photo permission" to "无照片访问权限",
        "No photos are indexed on this device. Create a photo index in generation settings first." to "此设备上暂无已索引的照片。请先在生成设置中创建照片索引。",
        "No response" to "无响应",
        "No search results" to "无搜索结果",
        "No speech was heard." to "未检测到语音。",
        "No thought text" to "无思考原文",
        "Notice" to "提示",
        "Notification permission" to "通知权限",
        "One moment" to "稍等片刻",
        "Open language list" to "打开语言列表",
        "Open model list" to "打开模型列表",
        "Open source licenses" to "开源许可",
        "Paused" to "已暂停",
        "Photo" to "照片",
        "Photo index cleared." to "照片索引已清除。",
        "Photo indexing" to "照片索引",
        "Photo indexing is complete." to "照片索引已完成。",
        "Photos" to "照片",
        "Pins the app display language." to "固定应用的显示语言。",
        "Plan a weekend trip for me." to "帮我制定一份周末旅行计划。",
        "Play sound effects during model responses" to "模型回答时播放音效",
        "Plays a gentle tap sound on new content chunks and tool calls." to "在收到新文本内容或调用工具时播放轻柔的敲击音效。",
        "Plays a sound as each new response text chunk arrives." to "每次收到新的回答文本片段时播放提示音。",
        "Pre-warm model (faster first reply)" to "预热模型 (加快首次响应)",
        "Prepares the model when the screen turns on to speed up initial generation." to "亮屏时预先准备模型，以加快首次响应速度。",
        "Prompt API requirements and device support" to "Prompt API 要求及支持设备",
        "Ready" to "已就绪",
        "Ready on device" to "端侧已就绪",
        "Ready with a lightweight model" to "轻量模型已就绪",
        "Rename" to "重命名",
        "Rename chat" to "重命名对话",
        "Requires photo access to search device photos." to "搜索设备中的照片需要照片访问权限。",
        "Reset all settings to default values?" to "是否将所有设置恢复为默认值？",
        "Reset to defaults" to "恢复默认",
        "Retry" to "重试",
        "Run this tool?" to "是否运行此工具？",
        "Save" to "保存",
        "Save edit" to "保存编辑",
        "Search" to "搜索",
        "Search chats" to "搜索对话",
        "See also" to "参考链接",
        " See the code sample on screen. " to " 请在屏幕上查看代码示例。 ",
        "Send" to "发送",
        "Send a message" to "发送消息",
        "Set up the Gemma 4 model first." to "请先配置 Gemma 4 模型。",
        "Settings" to "设置",
        "Speak" to "说话",
        "Speak whenever you're ready" to "请随时说话",
        "Speaking" to "正在回答",
        "Speaking…" to "正在回答…",
        "Speech recognition error" to "语音识别错误",
        "Speech recognition is busy. Please try again soon." to "语音识别服务正忙，请稍后重试。",
        "Speech recognition is unavailable on this device." to "此设备不支持语音识别功能。",
        "Speech recognition is unavailable." to "语音识别不可用。",
        "Start" to "开始",
        "Start a new chat" to "开启新对话",
        "Start editing" to "开始编辑",
        "Start indexing" to "开始创建索引",
        "Start indexing photos?" to "是否开始创建照片索引？",
        "Start photo indexing" to "开始照片索引",
        "Status unknown" to "状态未知",
        "Stop" to "停止",
        "Stop photo indexing" to "停止照片索引",
        "Stop response" to "停止回答",
        "Stopped" to "已停止",
        "Stopping" to "正在停止",
        "Stopping partway keeps the photos already indexed and resumes from there next time." to "中途停止会保留已索引的照片，下次将继续进行。",
        "Stopping photo indexing" to "正在停止照片索引",
        "Suggest an easy and delicious dinner I can make." to "推荐一个简单美味、容易下厨的晚餐菜谱。",
        "Suggest one fun idea I could try today." to "给我推荐一个今天可以尝试的有趣点子。",
        "Supported device list" to "支持设备列表",
        "System instructions and pre-prompt" to "系统指令与预设 Prompt",
        "Tap again to speak." to "请再次轻触以说话。",
        "Tap the microphone to start" to "轻触麦克风开始",
        "Tap to cancel" to "轻触取消",
        "Tap to finish" to "轻触完成",
        "Tap to speak" to "轻触说话",
        "Tap to stop" to "轻触停止",
        "Tell me one interesting and useful science or everyday fact." to "告诉我一个有趣且实用的科学或生活冷知识。",
        "Temperature (0.00-1.00)" to "温度 (0.00-1.00)",
        "The index was cleared." to "索引已清除。",
        "The model could not be prepared" to "无法准备模型",
        "The model is answering" to "模型正在回答",
        "The model is downloading." to "正在下载模型。",
        "The model is unavailable on this device" to "此设备无法使用此模型",
        "The model's thoughts appear here." to "模型的思考过程将显示在此处。",
        "The on-device model is unavailable on this device." to "此设备不支持端侧模型。",
        "The response was empty. Please try again." to "回答内容为空，请重试。",
        "The user cancelled the run." to "用户取消了运行。",
        "There is no message to send." to "没有可发送的消息。",
        "Thinking" to "思考中",
        "Thinking is skipped for voice replies · setting is kept" to "语音对话时暂不展开 Thinking · 设置保持不变",
        "Thinking mode" to "深度思考模式",
        "Thinking stopped" to "已停止思考",
        "Thinking unavailable" to "思考功能不可用",
        "This response has no thought text to show." to "此回答没有可展示的思考过程。",
        "This tool and argument combination was already tried once, so it was not retried." to "已尝试过此工具与参数组合，不再重复重试。",
        "Thought complete" to "思考完毕",
        "Tool execution timed out" to "工具执行超时",
        "Top K (1-128)" to "Top K (1-128)",
        "Trip plan" to "旅行计划",
        "Unavailable" to "不可用",
        "Unknown" to "未知",
        "Unknown date" to "日期未知",
        "User" to "用户",
        "Using a tool" to "正在使用工具",
        "Version" to "版本",
        "Vibrates on response start, completion, and tool calls." to "在回复开始、完成以及调用工具时触发振动。",
        "Vibration feedback" to "振动反馈",
        "Voice chat" to "语音对话",
        "Voice chat needs microphone permission." to "语音对话需要麦克风权限。",
        "Voice mode" to "语音模式",
        "Warm up model in advance" to "预热模型",
        "Wi-Fi is recommended" to "建议使用 Wi-Fi",
        "Writing a reply" to "正在生成回答",
        "all" to "全部",
        "error: Photo search could not complete because of a local index or on-device embedding error. Do not repeat the same search; tell the user photo search failed." to "错误：由于本地索引或端侧向量嵌入错误，照片搜索无法完成。请勿重复同一搜索，直接告知用户搜索失败。",
        "error: after cannot be later than before" to "错误：after 不能晚于 before",
        "error: after must be YYYY-MM-DD" to "错误：after 必须为 YYYY-MM-DD 格式",
        "error: before must be YYYY-MM-DD" to "错误：before 必须为 YYYY-MM-DD 格式",
        "error: could not open an HTTPS connection" to "错误：无法打开 HTTPS 连接",
        "error: could not resolve the host" to "错误：无法解析主机地址",
        "error: local or private network addresses cannot be fetched" to "错误：无法获取本地或局域网私有地址",
        "error: only public HTTPS URLs are allowed" to "错误：仅允许公开的 HTTPS URL",
        "error: query is required." to "错误：必须提供搜索关键词。",
        "error: query is required. e.g. search_conversations(query=\"coffee\", limit=5)" to "错误：必须提供 query。示例：search_conversations(query=\"咖啡\", limit=5)",
        "error: query must be 200 characters or fewer" to "错误：query 长度必须在 200 字符以内",
        "error: redirect handling failed" to "错误：重定向处理失败",
        "error: the photo search query must be 120 characters or fewer." to "错误：照片搜索关键词必须在 120 字符以内。",
        "error: url is required. e.g. web_fetch(url=\"https://example.com\")" to "错误：必须提供 url。示例：web_fetch(url=\"https://example.com\")",
        " link " to " 链接 ",
    )

    private val ES = mapOf(
        " · applies to the next response" to " · se aplica a la próxima respuesta",
        " · tools supported" to " · herramientas admitidas",
        " · tools unavailable" to " · herramientas no disponibles",
        "(no message)" to "(sin mensaje)",
        "A response is still in progress." to "Una respuesta aún está en curso.",
        "A short vibration marks each new response text chunk." to "Una breve vibración indica cada nuevo fragmento de texto de respuesta.",
        "About Justagem" to "Acerca de Justagem",
        "All vector extraction and photo analysis run on this device and are never sent to a server. That’s one of the best things about using an on-device model." to "Toda la extracción de vectores y el análisis de fotos se ejecutan en este dispositivo y nunca se envían a un servidor. Esa es una de las mayores ventajas de usar un modelo en el dispositivo.",
        "Allow" to "Permitir",
        "An on-device model computes a vector for each photo, so it can take several minutes and use battery. " to "Un modelo en el dispositivo calcula un vector para cada foto, por lo que puede tardar varios minutos y consumir batería. ",
        "Analyzing photos" to "Analizando fotos",
        "Are you sure you want to delete this chat? This action cannot be undone." to "¿Seguro que deseas eliminar este chat? Esta acción no se puede deshacer.",
        "Ask me anything" to "Pregúntame lo que quieras",
        "Attach an image" to "Adjuntar una imagen",
        "Attach image" to "Adjuntar imagen",
        "Available" to "Disponible",
        "Back" to "Atrás",
        "Camera" to "Cámara",
        "Cancel" to "Cancelar",
        "Chats" to "Chats",
        "Check again" to "Volver a comprobar",
        "Check status again" to "Comprobar estado de nuevo",
        "Check the speech recognition connection." to "Comprueba la conexión del reconocimiento de voz.",
        "Checking" to "Comprobando",
        "Checking the model status." to "Comprobando el estado del modelo.",
        "Clear existing index data?" to "¿Borrar los datos de índice existentes?",
        "Clear index" to "Borrar índice",
        "Clear photo index" to "Borrar índice de fotos",
        "Clear search" to "Borrar búsqueda",
        "Close" to "Cerrar",
        "Close full thoughts" to "Cerrar pensamientos completos",
        "Close language list" to "Cerrar lista de idiomas",
        "Close model list" to "Cerrar lista de modelos",
        "Close voice chat" to "Cerrar chat de voz",
        "Co-Engineered by:" to "Co-desarrollado por:",
        "Complete" to "Completado",
        "Continuing with a normal response." to "Continuando con una respuesta normal.",
        "Conversations" to "Conversaciones",
        "Copied" to "Copiado",
        "Copy" to "Copiar",
        "Could not build a response. Please try again." to "No se pudo generar una respuesta. Inténtalo de nuevo.",
        "Could not fully clear the index." to "No se pudo borrar completamente el índice.",
        "Could not prepare speech recognition." to "No se pudo preparar el reconocimiento de voz.",
        "Could not read the saved index. Please retry." to "No se pudo leer el índice guardado. Inténtalo de nuevo.",
        "Could not save the conversation. Check available storage." to "No se pudo guardar la conversación. Comprueba el almacenamiento disponible.",
        "Could not save the index. Please retry." to "No se pudo guardar el índice. Inténtalo de nuevo.",
        "Could not start speech recognition." to "No se pudo iniciar el reconocimiento de voz.",
        "Currently selected" to "Seleccionado actualmente",
        "Defaults" to "Predeterminados",
        "Delete" to "Eliminar",
        "Delete all" to "Eliminar todo",
        "Delete chat" to "Eliminar chat",
        "Dinner idea" to "Idea para cenar",
        "Download" to "Descargar",
        "Download model" to "Descargar modelo",
        "Download needed" to "Descarga necesaria",
        "Download required" to "Descarga requerida",
        "Downloading" to "Descargando",
        "Downloading model" to "Descargando modelo",
        "E4B ready" to "E4B listo",
        "Edit" to "Editar",
        "Edit your message" to "Editar mensaje",
        "EmbeddingGemma 2 model could not be initialized (check AICore)." to "No se pudo inicializar el modelo EmbeddingGemma 2 (comprueba AICore).",
        "Engineered by:" to "Desarrollado por:",
        "Enroll in the AICore Developer Preview (get Gemini Nano v4 early)" to "Inscríbete en la vista previa para desarrolladores de AICore (obtén Gemini Nano v4 antes)",
        "Exit voice mode" to "Salir del modo de voz",
        "Feedback" to "Comentarios",
        "Fetch this web page?" to "¿Obtener esta página web?",
        "Follow system language" to "Seguir el idioma del sistema",
        "Full thoughts" to "Pensamientos completos",
        "Fun fact" to "Dato curioso",
        "General" to "General",
        "Generation stopped." to "Generación detenida.",
        "HTTPS request failed" to "Error en la solicitud HTTPS",
        "Haptics during model responses" to "Vibración durante respuestas del modelo",
        "I couldn't finish the combined answer, so here are the per-photo analyses." to "No pude terminar la respuesta combinada, así que aquí están los análisis por foto.",
        "I didn't catch that. Please say it again." to "No lo he entendido. Por favor, dillo de nuevo.",
        "I didn't catch that. Please try again." to "No lo he entendido. Por favor, inténtalo de nuevo.",
        "Idea" to "Idea",
        "Idle" to "Inactivo",
        "Image analysis" to "Análisis de imagen",
        "Index" to "Indexar",
        "Indexed photos" to "Fotos indexadas",
        "Indexing failed. The saved index was kept." to "Error al indexar. Se mantuvo el índice guardado.",
        "Indexing photos" to "Indexando fotos",
        "Indexing was interrupted." to "La indexación fue interrumpida.",
        "Just a moment…" to "Un momento…",
        "Language" to "Idioma",
        "Lightweight model instead of E4B" to "Modelo ligero en lugar de E4B",
        "Listening" to "Escuchando",
        "Listening…" to "Escuchando…",
        "Loading photo index" to "Cargando índice de fotos",
        "Loads the model onto the accelerator in advance so the first reply starts sooner. May use slightly more battery." to "Carga el modelo en el acelerador con antelación para que la primera respuesta comience antes. Puede usar un poco más de batería.",
        "Max output tokens (128-4096)" to "Tokens máximos de salida (128-4096)",
        "Microphone permission is required." to "Se requiere permiso de micrófono.",
        "Model" to "Modelo",
        "Model needs setup" to "El modelo necesita configuración",
        "Model settings" to "Ajustes del modelo",
        "Model unavailable" to "Modelo no disponible",
        "Needs attention" to "Requiere atención",
        "Needs download" to "Requiere descarga",
        "New chat" to "Nuevo chat",
        "No chats yet" to "Aún no hay chats",
        "No matching chats" to "No hay chats coincidentes",
        "No photo permission" to "Sin permiso de fotos",
        "No photos are indexed on this device. Create a photo index in generation settings first." to "No hay fotos indexadas en este dispositivo. Crea primero un índice de fotos en los ajustes de generación.",
        "No response" to "Sin respuesta",
        "No search results" to "Sin resultados de búsqueda",
        "No speech was heard." to "No se detectó voz.",
        "No thought text" to "Sin texto de pensamiento",
        "Notice" to "Aviso",
        "Notification permission" to "Permiso de notificaciones",
        "One moment" to "Un momento",
        "Open language list" to "Abrir lista de idiomas",
        "Open model list" to "Abrir lista de modelos",
        "Open source licenses" to "Licencias de código abierto",
        "Paused" to "Pausado",
        "Photo" to "Foto",
        "Photo index cleared." to "Índice de fotos borrado.",
        "Photo indexing" to "Indexación de fotos",
        "Photo indexing is complete." to "La indexación de fotos ha finalizado.",
        "Photos" to "Fotos",
        "Pins the app display language." to "Fija el idioma de visualización de la aplicación.",
        "Plan a weekend trip for me." to "Planifica un viaje de fin de semana para mí.",
        "Play sound effects during model responses" to "Reproducir efectos de sonido durante las respuestas",
        "Plays a gentle tap sound on new content chunks and tool calls." to "Reproduce un suave sonido de toque en nuevos fragmentos y llamadas a herramientas.",
        "Plays a sound as each new response text chunk arrives." to "Reproduce un sonido cuando llega cada nuevo fragmento de texto de respuesta.",
        "Pre-warm model (faster first reply)" to "Precalentar modelo (primera respuesta más rápida)",
        "Prepares the model when the screen turns on to speed up initial generation." to "Prepara el modelo al encender la pantalla para acelerar la generación inicial.",
        "Prompt API requirements and device support" to "Requisitos y compatibilidad de dispositivos para la API Prompt",
        "Ready" to "Listo",
        "Ready on device" to "Listo en el dispositivo",
        "Ready with a lightweight model" to "Listo con modelo ligero",
        "Rename" to "Cambiar nombre",
        "Rename chat" to "Cambiar nombre del chat",
        "Requires photo access to search device photos." to "Requiere acceso a las fotos para buscar en el dispositivo.",
        "Reset all settings to default values?" to "¿Restablecer todos los ajustes a los valores predeterminados?",
        "Reset to defaults" to "Restablecer valores",
        "Retry" to "Reintentar",
        "Run this tool?" to "¿Ejecutar esta herramienta?",
        "Save" to "Guardar",
        "Save edit" to "Guardar edición",
        "Search" to "Buscar",
        "Search chats" to "Buscar chats",
        "See also" to "Ver también",
        " See the code sample on screen. " to " Consulta el ejemplo de código en pantalla. ",
        "Send" to "Enviar",
        "Send a message" to "Enviar un mensaje",
        "Set up the Gemma 4 model first." to "Configura primero el modelo Gemma 4.",
        "Settings" to "Ajustes",
        "Speak" to "Hablar",
        "Speak whenever you're ready" to "Habla cuando estés listo",
        "Speaking" to "Hablando",
        "Speaking…" to "Hablando…",
        "Speech recognition error" to "Error de reconocimiento de voz",
        "Speech recognition is busy. Please try again soon." to "El reconocimiento de voz está ocupado. Inténtalo de nuevo pronto.",
        "Speech recognition is unavailable on this device." to "El reconocimiento de voz no está disponible en este dispositivo.",
        "Speech recognition is unavailable." to "El reconocimiento de voz no está disponible.",
        "Start" to "Iniciar",
        "Start a new chat" to "Iniciar un nuevo chat",
        "Start editing" to "Iniciar edición",
        "Start indexing" to "Iniciar indexación",
        "Start indexing photos?" to "¿Iniciar la indexación de fotos?",
        "Start photo indexing" to "Iniciar indexación de fotos",
        "Status unknown" to "Estado desconocido",
        "Stop" to "Detener",
        "Stop photo indexing" to "Detener indexación de fotos",
        "Stop response" to "Detener respuesta",
        "Stopped" to "Detenido",
        "Stopping" to "Deteniendo",
        "Stopping partway keeps the photos already indexed and resumes from there next time." to "Detenerse a mitad de camino conserva las fotos ya indexadas y continúa desde allí la próxima vez.",
        "Stopping photo indexing" to "Deteniendo indexación de fotos",
        "Suggest an easy and delicious dinner I can make." to "Sugiere una cena fácil y deliciosa que pueda preparar.",
        "Suggest one fun idea I could try today." to "Sugiere una idea divertida que pueda probar hoy.",
        "Supported device list" to "Lista de dispositivos compatibles",
        "System instructions and pre-prompt" to "Instrucciones del sistema y pre-indicación",
        "Tap again to speak." to "Toca de nuevo para hablar.",
        "Tap the microphone to start" to "Toca el micrófono para comenzar",
        "Tap to cancel" to "Toca para cancelar",
        "Tap to finish" to "Toca para finalizar",
        "Tap to speak" to "Toca para hablar",
        "Tap to stop" to "Toca para detener",
        "Tell me one interesting and useful science or everyday fact." to "Cuéntame un dato científico o cotidiano interesante y útil.",
        "Temperature (0.00-1.00)" to "Temperatura (0.00-1.00)",
        "The index was cleared." to "Se ha borrado el índice.",
        "The model could not be prepared" to "No se pudo preparar el modelo",
        "The model is answering" to "El modelo está respondiendo",
        "The model is downloading." to "El modelo se está descargando.",
        "The model is unavailable on this device" to "El modelo no está disponible en este dispositivo",
        "The model's thoughts appear here." to "Los pensamientos del modelo aparecen aquí.",
        "The on-device model is unavailable on this device." to "El modelo en el dispositivo no está disponible en este dispositivo.",
        "The response was empty. Please try again." to "La respuesta estaba vacía. Inténtalo de nuevo.",
        "The user cancelled the run." to "El usuario canceló la ejecución.",
        "There is no message to send." to "No hay mensaje para enviar.",
        "Thinking" to "Pensando",
        "Thinking is skipped for voice replies · setting is kept" to "El pensamiento se omite en respuestas de voz · se mantiene el ajuste",
        "Thinking mode" to "Modo de pensamiento",
        "Thinking stopped" to "Pensamiento detenido",
        "Thinking unavailable" to "Pensamiento no disponible",
        "This response has no thought text to show." to "Esta respuesta no tiene texto de pensamiento.",
        "This tool and argument combination was already tried once, so it was not retried." to "Esta combinación de herramienta y argumento ya se probó una vez, por lo que no se reintentó.",
        "Thought complete" to "Pensamiento completado",
        "Tool execution timed out" to "Se agotó el tiempo de ejecución de la herramienta",
        "Top K (1-128)" to "Top K (1-128)",
        "Trip plan" to "Plan de viaje",
        "Unavailable" to "No disponible",
        "Unknown" to "Desconocido",
        "Unknown date" to "Fecha desconocida",
        "User" to "Usuario",
        "Using a tool" to "Usando una herramienta",
        "Version" to "Versión",
        "Vibrates on response start, completion, and tool calls." to "Vibra al iniciar y completar respuestas y llamadas a herramientas.",
        "Vibration feedback" to "Respuesta táctil",
        "Voice chat" to "Chat de voz",
        "Voice chat needs microphone permission." to "El chat de voz necesita permiso de micrófono.",
        "Voice mode" to "Modo de voz",
        "Warm up model in advance" to "Precalentar el modelo con antelación",
        "Wi-Fi is recommended" to "Se recomienda Wi-Fi",
        "Writing a reply" to "Escribiendo respuesta",
        "all" to "todo",
        "error: Photo search could not complete because of a local index or on-device embedding error. Do not repeat the same search; tell the user photo search failed." to "error: La búsqueda de fotos no se pudo completar debido a un error de índice local o incrustación en el dispositivo. No repitas la misma búsqueda; informa al usuario de que la búsqueda falló.",
        "error: after cannot be later than before" to "error: after no puede ser posterior a before",
        "error: after must be YYYY-MM-DD" to "error: after debe tener el formato YYYY-MM-DD",
        "error: before must be YYYY-MM-DD" to "error: before debe tener el formato YYYY-MM-DD",
        "error: could not open an HTTPS connection" to "error: no se pudo abrir la conexión HTTPS",
        "error: could not resolve the host" to "error: no se pudo resolver el host",
        "error: local or private network addresses cannot be fetched" to "error: no se pueden recuperar direcciones locales o privadas",
        "error: only public HTTPS URLs are allowed" to "error: solo se permiten URLs HTTPS públicas",
        "error: query is required." to "error: se requiere un término de búsqueda.",
        "error: query is required. e.g. search_conversations(query=\"coffee\", limit=5)" to "error: se requiere query. ej.: search_conversations(query=\"café\", limit=5)",
        "error: query must be 200 characters or fewer" to "error: query debe tener 200 caracteres o menos",
        "error: redirect handling failed" to "error: error al procesar la redirección",
        "error: the photo search query must be 120 characters or fewer." to "error: la consulta de búsqueda de fotos debe tener 120 caracteres o menos.",
        "error: url is required. e.g. web_fetch(url=\"https://example.com\")" to "error: se requiere url. ej.: web_fetch(url=\"https://example.com\")",
        " link " to " enlace ",
    )

    private val DE = mapOf(
        " · applies to the next response" to " · gilt ab der nächsten Antwort",
        " · tools supported" to " · Tools unterstützt",
        " · tools unavailable" to " · Tools nicht verfügbar",
        "(no message)" to "(keine Nachricht)",
        "A response is still in progress." to "Eine Antwort ist noch in Bearbeitung.",
        "A short vibration marks each new response text chunk." to "Eine kurze Vibration kennzeichnet jedes neue Antworttext-Segment.",
        "About Justagem" to "Über Justagem",
        "All vector extraction and photo analysis run on this device and are never sent to a server. That’s one of the best things about using an on-device model." to "Die gesamte Vektorextraktion und Fotoanalyse wird auf diesem Gerät ausgeführt und niemals an einen Server gesendet. Das ist einer der besten Vorteile eines On-Device-Modells.",
        "Allow" to "Zulassen",
        "An on-device model computes a vector for each photo, so it can take several minutes and use battery. " to "Ein On-Device-Modell berechnet für jedes Foto einen Vektor, was einige Minuten dauern und Akku verbrauchen kann. ",
        "Analyzing photos" to "Fotos werden analysiert",
        "Are you sure you want to delete this chat? This action cannot be undone." to "Möchtest du diesen Chat wirklich löschen? Dies kann nicht rückgängig gemacht werden.",
        "Ask me anything" to "Frag mich etwas",
        "Attach an image" to "Bild anhängen",
        "Attach image" to "Bild anhängen",
        "Available" to "Verfügbar",
        "Back" to "Zurück",
        "Camera" to "Kamera",
        "Cancel" to "Abbrechen",
        "Chats" to "Chats",
        "Check again" to "Erneut prüfen",
        "Check status again" to "Status erneut prüfen",
        "Check the speech recognition connection." to "Bitte die Spracherkennungsverbindung prüfen.",
        "Checking" to "Prüfen",
        "Checking the model status." to "Modellstatus wird überprüft.",
        "Clear existing index data?" to "Vorhandene Indexdaten löschen?",
        "Clear index" to "Index löschen",
        "Clear photo index" to "Foto-Index löschen",
        "Clear search" to "Suche löschen",
        "Close" to "Schließen",
        "Close full thoughts" to "Vollständige Gedanken schließen",
        "Close language list" to "Sprachliste schließen",
        "Close model list" to "Modellliste schließen",
        "Close voice chat" to "Sprach-Chat schließen",
        "Co-Engineered by:" to "Gemeinsam entwickelt von:",
        "Complete" to "Abgeschlossen",
        "Continuing with a normal response." to "Fortfahren mit einer normalen Antwort.",
        "Conversations" to "Unterhaltungen",
        "Copied" to "Kopiert",
        "Copy" to "Kopieren",
        "Could not build a response. Please try again." to "Antwort konnte nicht erstellt werden. Bitte erneut versuchen.",
        "Could not fully clear the index." to "Index konnte nicht vollständig gelöscht werden.",
        "Could not prepare speech recognition." to "Spracherkennung konnte nicht vorbereitet werden.",
        "Could not read the saved index. Please retry." to "Gespeicherter Index konnte nicht gelesen werden. Bitte erneut versuchen.",
        "Could not save the conversation. Check available storage." to "Unterhaltung konnte nicht gespeichert werden. Speicherplatz prüfen.",
        "Could not save the index. Please retry." to "Index konnte nicht gespeichert werden. Bitte erneut versuchen.",
        "Could not start speech recognition." to "Spracherkennung konnte nicht gestartet werden.",
        "Currently selected" to "Aktuell ausgewählt",
        "Defaults" to "Standard",
        "Delete" to "Löschen",
        "Delete all" to "Alles löschen",
        "Delete chat" to "Chat löschen",
        "Dinner idea" to "Abendessen-Idee",
        "Download" to "Herunterladen",
        "Download model" to "Modell herunterladen",
        "Download needed" to "Download erforderlich",
        "Download required" to "Download erforderlich",
        "Downloading" to "Wird heruntergeladen",
        "Downloading model" to "Modell wird heruntergeladen",
        "E4B ready" to "E4B bereit",
        "Edit" to "Bearbeiten",
        "Edit your message" to "Nachricht bearbeiten",
        "EmbeddingGemma 2 model could not be initialized (check AICore)." to "EmbeddingGemma 2-Modell konnte nicht initialisiert werden (AICore prüfen).",
        "Engineered by:" to "Entwickelt von:",
        "Enroll in the AICore Developer Preview (get Gemini Nano v4 early)" to "Beim AICore Developer Preview anmelden (Gemini Nano v4 vorab erhalten)",
        "Exit voice mode" to "Sprachmodus beenden",
        "Feedback" to "Feedback",
        "Fetch this web page?" to "Diese Webseite abrufen?",
        "Follow system language" to "Systemsprache folgen",
        "Full thoughts" to "Vollständige Gedanken",
        "Fun fact" to "Wissenswertes",
        "General" to "Allgemein",
        "Generation stopped." to "Generierung gestoppt.",
        "HTTPS request failed" to "HTTPS-Anfrage fehlgeschlagen",
        "Haptics during model responses" to "Haptik bei Modellantworten",
        "I couldn't finish the combined answer, so here are the per-photo analyses." to "Die kombinierte Antwort konnte nicht abgeschlossen werden; hier sind die Einzelanalysen pro Foto.",
        "I didn't catch that. Please say it again." to "Das habe ich nicht verstanden. Bitte wiederholen.",
        "I didn't catch that. Please try again." to "Das habe ich nicht verstanden. Bitte erneut versuchen.",
        "Idea" to "Idee",
        "Idle" to "Im Leerlauf",
        "Image analysis" to "Bildanalyse",
        "Index" to "Indizieren",
        "Indexed photos" to "Indizierte Fotos",
        "Indexing failed. The saved index was kept." to "Indizierung fehlgeschlagen. Der gespeicherte Index wurde beibehalten.",
        "Indexing photos" to "Fotos werden indiziert",
        "Indexing was interrupted." to "Indizierung wurde unterbrochen.",
        "Just a moment…" to "Einen Moment…",
        "Language" to "Sprache",
        "Lightweight model instead of E4B" to "Leichtes Modell statt E4B",
        "Listening" to "Zuhören",
        "Listening…" to "Zuhören…",
        "Loading photo index" to "Foto-Index wird geladen",
        "Loads the model onto the accelerator in advance so the first reply starts sooner. May use slightly more battery." to "Lädt das Modell vorab auf den Beschleuniger, damit die erste Antwort schneller beginnt. Kann etwas mehr Akku verbrauchen.",
        "Max output tokens (128-4096)" to "Maximale Ausgabetoken (128-4096)",
        "Microphone permission is required." to "Mikrofonberechtigung ist erforderlich.",
        "Model" to "Modell",
        "Model needs setup" to "Modell erfordert Einrichtung",
        "Model settings" to "Modelleinstellungen",
        "Model unavailable" to "Modell nicht verfügbar",
        "Needs attention" to "Achtung erforderlich",
        "Needs download" to "Download erforderlich",
        "New chat" to "Neuer Chat",
        "No chats yet" to "Noch keine Chats",
        "No matching chats" to "Keine passenden Chats",
        "No photo permission" to "Keine Foto-Berechtigung",
        "No photos are indexed on this device. Create a photo index in generation settings first." to "Auf diesem Gerät sind keine Fotos indiziert. Erstelle zuerst einen Foto-Index in den Generierungseinstellungen.",
        "No response" to "Keine Antwort",
        "No search results" to "Keine Suchergebnisse",
        "No speech was heard." to "Es wurde keine Sprache erkannt.",
        "No thought text" to "Kein Gedankentext",
        "Notice" to "Hinweis",
        "Notification permission" to "Benachrichtigungsberechtigung",
        "One moment" to "Einen Moment",
        "Open language list" to "Sprachliste öffnen",
        "Open model list" to "Modellliste öffnen",
        "Open source licenses" to "Open-Source-Lizenzen",
        "Paused" to "Pausiert",
        "Photo" to "Foto",
        "Photo index cleared." to "Foto-Index gelöscht.",
        "Photo indexing" to "Foto-Indizierung",
        "Photo indexing is complete." to "Foto-Indizierung ist abgeschlossen.",
        "Photos" to "Fotos",
        "Pins the app display language." to "Legt die Anzeigesprache der App fest.",
        "Plan a weekend trip for me." to "Plane einen Wochenendausflug für mich.",
        "Play sound effects during model responses" to "Soundeffekte bei Modellantworten abspielen",
        "Plays a gentle tap sound on new content chunks and tool calls." to "Spielt einen sanften Tipp-Ton bei neuen Inhalten und Tool-Aufrufen ab.",
        "Plays a sound as each new response text chunk arrives." to "Spielt einen Ton ab, wenn jedes neue Antworttext-Segment eintrifft.",
        "Pre-warm model (faster first reply)" to "Modell vorwärmen (schnellere erste Antwort)",
        "Prepares the model when the screen turns on to speed up initial generation." to "Bereitet das Modell beim Einschalten vor, um die erste Generierung zu beschleunigen.",
        "Prompt API requirements and device support" to "Prompt-API-Anforderungen und Geräteunterstützung",
        "Ready" to "Bereit",
        "Ready on device" to "Auf dem Gerät bereit",
        "Ready with a lightweight model" to "Bereit mit leichtem Modell",
        "Rename" to "Umbenennen",
        "Rename chat" to "Chat umbenennen",
        "Requires photo access to search device photos." to "Erfordert Fotozugriff, um Gerätefotos zu durchsuchen.",
        "Reset all settings to default values?" to "Alle Einstellungen auf die Standardwerte zurücksetzen?",
        "Reset to defaults" to "Auf Standard zurücksetzen",
        "Retry" to "Wiederholen",
        "Run this tool?" to "Dieses Tool ausführen?",
        "Save" to "Speichern",
        "Save edit" to "Bearbeitung speichern",
        "Search" to "Suchen",
        "Search chats" to "Chats durchsuchen",
        "See also" to "Siehe auch",
        " See the code sample on screen. " to " Siehe den Beispielcode auf dem Bildschirm. ",
        "Send" to "Senden",
        "Send a message" to "Nachricht senden",
        "Set up the Gemma 4 model first." to "Richte zuerst das Gemma 4-Modell ein.",
        "Settings" to "Einstellungen",
        "Speak" to "Sprechen",
        "Speak whenever you're ready" to "Sprich, wann immer du bereit bist",
        "Speaking" to "Sprechen",
        "Speaking…" to "Sprechen…",
        "Speech recognition error" to "Spracherkennungsfehler",
        "Speech recognition is busy. Please try again soon." to "Spracherkennung ist belegt. Bitte versuche es bald erneut.",
        "Speech recognition is unavailable on this device." to "Spracherkennung ist auf diesem Gerät nicht verfügbar.",
        "Speech recognition is unavailable." to "Spracherkennung ist nicht verfügbar.",
        "Start" to "Starten",
        "Start a new chat" to "Neuen Chat starten",
        "Start editing" to "Bearbeitung starten",
        "Start indexing" to "Indizierung starten",
        "Start indexing photos?" to "Foto-Indizierung starten?",
        "Start photo indexing" to "Foto-Indizierung starten",
        "Status unknown" to "Status unbekannt",
        "Stop" to "Stoppen",
        "Stop photo indexing" to "Foto-Indizierung stoppen",
        "Stop response" to "Antwort stoppen",
        "Stopped" to "Gestoppt",
        "Stopping" to "Stoppen",
        "Stopping partway keeps the photos already indexed and resumes from there next time." to "Ein vorzeitiger Stopp behält bereits indizierte Fotos bei und setzt beim nächsten Mal dort fort.",
        "Stopping photo indexing" to "Foto-Indizierung wird gestoppt",
        "Suggest an easy and delicious dinner I can make." to "Schlage ein einfaches und leckeres Abendessen vor, das ich kochen kann.",
        "Suggest one fun idea I could try today." to "Schlage eine unterhaltsame Idee vor, die ich heute ausprobieren kann.",
        "Supported device list" to "Liste unterstützter Geräte",
        "System instructions and pre-prompt" to "Systemanweisungen und Pre-Prompt",
        "Tap again to speak." to "Erneut tippen zum Sprechen.",
        "Tap the microphone to start" to "Mikrofon zum Starten tippen",
        "Tap to cancel" to "Zum Abbrechen tippen",
        "Tap to finish" to "Zum Beenden tippen",
        "Tap to speak" to "Zum Sprechen tippen",
        "Tap to stop" to "Zum Stoppen tippen",
        "Tell me one interesting and useful science or everyday fact." to "Erzähle mir einen interessanten und nützlichen wissenschaftlichen oder alltäglichen Fakt.",
        "Temperature (0.00-1.00)" to "Temperatur (0.00-1.00)",
        "The index was cleared." to "Der Index wurde gelöscht.",
        "The model could not be prepared" to "Modell konnte nicht vorbereitet werden",
        "The model is answering" to "Das Modell antwortet gerade",
        "The model is downloading." to "Das Modell wird heruntergeladen.",
        "The model is unavailable on this device" to "Das Modell ist auf diesem Gerät nicht verfügbar",
        "The model's thoughts appear here." to "Die Gedanken des Modells erscheinen hier.",
        "The on-device model is unavailable on this device." to "Das On-Device-Modell ist auf diesem Gerät nicht verfügbar.",
        "The response was empty. Please try again." to "Die Antwort war leer. Bitte erneut versuchen.",
        "The user cancelled the run." to "Der Benutzer hat die Ausführung abgebrochen.",
        "There is no message to send." to "Keine Nachricht zum Senden vorhanden.",
        "Thinking" to "Nachdenken",
        "Thinking is skipped for voice replies · setting is kept" to "Thinking wird bei Sprachantworten übersprungen · Einstellung bleibt erhalten",
        "Thinking mode" to "Denkmodus",
        "Thinking stopped" to "Nachdenken gestoppt",
        "Thinking unavailable" to "Denken nicht verfügbar",
        "This response has no thought text to show." to "Diese Antwort enthält keinen Gedankentext.",
        "This tool and argument combination was already tried once, so it was not retried." to "Diese Kombination aus Tool und Argument wurde bereits einmal versucht und daher nicht wiederholt.",
        "Thought complete" to "Gedanke abgeschlossen",
        "Tool execution timed out" to "Tool-Ausführungszeit abgelaufen",
        "Top K (1-128)" to "Top K (1-128)",
        "Trip plan" to "Reiseplan",
        "Unavailable" to "Nicht verfügbar",
        "Unknown" to "Unbekannt",
        "Unknown date" to "Unbekanntes Datum",
        "User" to "Benutzer",
        "Using a tool" to "Tool wird verwendet",
        "Version" to "Version",
        "Vibrates on response start, completion, and tool calls." to "Vibriert bei Antwortbeginn, -abschluss und Tool-Aufrufen.",
        "Vibration feedback" to "Vibrationsfeedback",
        "Voice chat" to "Sprach-Chat",
        "Voice chat needs microphone permission." to "Sprach-Chat erfordert Mikrofonberechtigung.",
        "Voice mode" to "Sprachmodus",
        "Warm up model in advance" to "Modell im Voraus aufwärmen",
        "Wi-Fi is recommended" to "WLAN wird empfohlen",
        "Writing a reply" to "Antwort wird verfasst",
        "all" to "alle",
        "error: Photo search could not complete because of a local index or on-device embedding error. Do not repeat the same search; tell the user photo search failed." to "Fehler: Die Fotosuche konnte aufgrund eines lokalen Index- oder On-Device-Einbettungsfehlers nicht abgeschlossen werden. Wiederhole die Suche nicht; informiere den Benutzer über den Fehlschlag.",
        "error: after cannot be later than before" to "Fehler: after darf nicht nach before liegen",
        "error: after must be YYYY-MM-DD" to "Fehler: after muss das Format JJJJ-MM-TT haben",
        "error: before must be YYYY-MM-DD" to "Fehler: before muss das Format JJJJ-MM-TT haben",
        "error: could not open an HTTPS connection" to "Fehler: HTTPS-Verbindung konnte nicht geöffnet werden",
        "error: could not resolve the host" to "Fehler: Host konnte nicht aufgelöst werden",
        "error: local or private network addresses cannot be fetched" to "Fehler: Lokale oder private Netzwerkadressen können nicht abgerufen werden",
        "error: only public HTTPS URLs are allowed" to "Fehler: Nur öffentliche HTTPS-URLs sind erlaubt",
        "error: query is required." to "Fehler: Suchbegriff ist erforderlich.",
        "error: query is required. e.g. search_conversations(query=\"coffee\", limit=5)" to "Fehler: query ist erforderlich. z. B. search_conversations(query=\"Kaffee\", limit=5)",
        "error: query must be 200 characters or fewer" to "Fehler: query darf maximal 200 Zeichen lang sein",
        "error: redirect handling failed" to "Fehler: Weiterleitungsverarbeitung fehlgeschlagen",
        "error: the photo search query must be 120 characters or fewer." to "Fehler: Die Fotosuchanfrage darf maximal 120 Zeichen lang sein.",
        "error: url is required. e.g. web_fetch(url=\"https://example.com\")" to "Fehler: url ist erforderlich. z. B. web_fetch(url=\"https://example.com\")",
        " link " to " Link ",
    )

    private val FR = mapOf(
        " · applies to the next response" to " · s'applique à la prochaine réponse",
        " · tools supported" to " · outils pris en charge",
        " · tools unavailable" to " · outils indisponibles",
        "(no message)" to "(aucun message)",
        "A response is still in progress." to "Une réponse est toujours en cours.",
        "A short vibration marks each new response text chunk." to "Une courte vibration signale chaque nouveau segment de texte de réponse.",
        "About Justagem" to "À propos de Justagem",
        "All vector extraction and photo analysis run on this device and are never sent to a server. That’s one of the best things about using an on-device model." to "Toute l'extraction de vecteurs et l'analyse de photos s'exécutent sur cet appareil et ne sont jamais envoyées à un serveur. C'est l'un des plus grands avantages d'un modèle sur l'appareil.",
        "Allow" to "Autoriser",
        "An on-device model computes a vector for each photo, so it can take several minutes and use battery. " to "Un modèle sur l'appareil calcule un vecteur pour chaque photo, ce qui peut prendre plusieurs minutes et consommer de la batterie. ",
        "Analyzing photos" to "Analyse des photos",
        "Are you sure you want to delete this chat? This action cannot be undone." to "Voulez-vous vraiment supprimer ce chat ? Cette action est irréversible.",
        "Ask me anything" to "Posez-moi une question",
        "Attach an image" to "Joindre une image",
        "Attach image" to "Joindre une image",
        "Available" to "Disponible",
        "Back" to "Retour",
        "Camera" to "Appareil photo",
        "Cancel" to "Annuler",
        "Chats" to "Discussions",
        "Check again" to "Vérifier à nouveau",
        "Check status again" to "Vérifier à nouveau le statut",
        "Check the speech recognition connection." to "Vérifiez la connexion de la reconnaissance vocale.",
        "Checking" to "Vérification",
        "Checking the model status." to "Vérification de l'état du modèle.",
        "Clear existing index data?" to "Effacer les données d'index existantes ?",
        "Clear index" to "Effacer l'index",
        "Clear photo index" to "Effacer l'index de photos",
        "Clear search" to "Effacer la recherche",
        "Close" to "Fermer",
        "Close full thoughts" to "Fermer les pensées complètes",
        "Close language list" to "Fermer la liste des langues",
        "Close model list" to "Fermer la liste des modèles",
        "Close voice chat" to "Fermer le chat vocal",
        "Co-Engineered by:" to "Co-conçu par :",
        "Complete" to "Terminé",
        "Continuing with a normal response." to "Poursuite avec une réponse normale.",
        "Conversations" to "Conversations",
        "Copied" to "Copié",
        "Copy" to "Copier",
        "Could not build a response. Please try again." to "Impossible de générer une réponse. Veuillez réessayer.",
        "Could not fully clear the index." to "Impossible d'effacer complètement l'index.",
        "Could not prepare speech recognition." to "Impossible de préparer la reconnaissance vocale.",
        "Could not read the saved index. Please retry." to "Impossible de lire l'index enregistré. Veuillez réessayer.",
        "Could not save the conversation. Check available storage." to "Impossible d'enregistrer la conversation. Vérifiez l'espace disponible.",
        "Could not save the index. Please retry." to "Impossible d'enregistrer l'index. Veuillez réessayer.",
        "Could not start speech recognition." to "Impossible de démarrer la reconnaissance vocale.",
        "Currently selected" to "Actuellement sélectionné",
        "Defaults" to "Par défaut",
        "Delete" to "Supprimer",
        "Delete all" to "Tout supprimer",
        "Delete chat" to "Supprimer le chat",
        "Dinner idea" to "Idée de dîner",
        "Download" to "Télécharger",
        "Download model" to "Télécharger le modèle",
        "Download needed" to "Téléchargement requis",
        "Download required" to "Téléchargement requis",
        "Downloading" to "Téléchargement",
        "Downloading model" to "Téléchargement du modèle en cours",
        "E4B ready" to "E4B prêt",
        "Edit" to "Modifier",
        "Edit your message" to "Modifier le message",
        "EmbeddingGemma 2 model could not be initialized (check AICore)." to "Impossible d'initialiser le modèle EmbeddingGemma 2 (vérifiez AICore).",
        "Engineered by:" to "Développé par :",
        "Enroll in the AICore Developer Preview (get Gemini Nano v4 early)" to "S'inscrire à la préversion développeur d'AICore (obtenir Gemini Nano v4 en avance)",
        "Exit voice mode" to "Quitter le mode vocal",
        "Feedback" to "Commentaires",
        "Fetch this web page?" to "Récupérer cette page web ?",
        "Follow system language" to "Langue du système",
        "Full thoughts" to "Pensées complètes",
        "Fun fact" to "Le saviez-vous",
        "General" to "Général",
        "Generation stopped." to "Génération arrêtée.",
        "HTTPS request failed" to "Échec de la requête HTTPS",
        "Haptics during model responses" to "Retour haptique lors des réponses du modèle",
        "I couldn't finish the combined answer, so here are the per-photo analyses." to "Impossible de finaliser la réponse globale, voici les analyses par photo.",
        "I didn't catch that. Please say it again." to "Je n'ai pas bien compris. Veuillez répéter.",
        "I didn't catch that. Please try again." to "Je n'ai pas bien compris. Veuillez réessayer.",
        "Idea" to "Idée",
        "Idle" to "En attente",
        "Image analysis" to "Analyse d'image",
        "Index" to "Indexer",
        "Indexed photos" to "Photos indexées",
        "Indexing failed. The saved index was kept." to "Échec de l'indexation. L'index enregistré a été conservé.",
        "Indexing photos" to "Indexation des photos",
        "Indexing was interrupted." to "L'indexation a été interrompue.",
        "Just a moment…" to "Un instant…",
        "Language" to "Langue",
        "Lightweight model instead of E4B" to "Modèle léger au lieu d'E4B",
        "Listening" to "Écoute",
        "Listening…" to "Écoute en cours…",
        "Loading photo index" to "Chargement de l'index de photos",
        "Loads the model onto the accelerator in advance so the first reply starts sooner. May use slightly more battery." to "Charge le modèle sur l'accélérateur à l'avance pour démarrer la première réponse plus vite. Peut consommer un peu plus de batterie.",
        "Max output tokens (128-4096)" to "Tokens de sortie max (128-4096)",
        "Microphone permission is required." to "L'autorisation du microphone est requise.",
        "Model" to "Modèle",
        "Model needs setup" to "Le modèle nécessite une configuration",
        "Model settings" to "Paramètres du modèle",
        "Model unavailable" to "Modèle non disponible",
        "Needs attention" to "Attention requise",
        "Needs download" to "Téléchargement requis",
        "New chat" to "Nouveau chat",
        "No chats yet" to "Pas encore de discussions",
        "No matching chats" to "Aucune discussion correspondante",
        "No photo permission" to "Aucune autorisation photo",
        "No photos are indexed on this device. Create a photo index in generation settings first." to "Aucune photo n'est indexée sur cet appareil. Créez d'abord un index de photos dans les paramètres de génération.",
        "No response" to "Aucune réponse",
        "No search results" to "Aucun résultat",
        "No speech was heard." to "Aucune voix n'a été entendue.",
        "No thought text" to "Aucun texte de réflexion",
        "Notice" to "Avis",
        "Notification permission" to "Autorisation de notification",
        "One moment" to "Un instant",
        "Open language list" to "Ouvrir la liste des langues",
        "Open model list" to "Ouvrir la liste des modèles",
        "Open source licenses" to "Licences open source",
        "Paused" to "En pause",
        "Photo" to "Photo",
        "Photo index cleared." to "Index de photos effacé.",
        "Photo indexing" to "Indexation des photos",
        "Photo indexing is complete." to "L'indexation des photos est terminée.",
        "Photos" to "Photos",
        "Pins the app display language." to "Définit la langue d'affichage de l'application.",
        "Plan a weekend trip for me." to "Planifie un voyage de week-end pour moi.",
        "Play sound effects during model responses" to "Jouer des effets sonores lors des réponses du modèle",
        "Plays a gentle tap sound on new content chunks and tool calls." to "Joue un son discret lors des nouveaux fragments de contenu et des appels d'outils.",
        "Plays a sound as each new response text chunk arrives." to "Joue un son à chaque arrivée d'un nouveau fragment de texte de réponse.",
        "Pre-warm model (faster first reply)" to "Préchauffer le modèle (première réponse plus rapide)",
        "Prepares the model when the screen turns on to speed up initial generation." to "Prépare le modèle à l'allumage pour accélérer la génération initiale.",
        "Prompt API requirements and device support" to "Conditions requises et compatibilité de l'API Prompt",
        "Ready" to "Prêt",
        "Ready on device" to "Prêt sur l'appareil",
        "Ready with a lightweight model" to "Prêt avec modèle léger",
        "Rename" to "Renommer",
        "Rename chat" to "Renommer la discussion",
        "Requires photo access to search device photos." to "Nécessite l'accès aux photos pour effectuer des recherches sur l'appareil.",
        "Reset all settings to default values?" to "Réinitialiser tous les paramètres aux valeurs par défaut ?",
        "Reset to defaults" to "Rétablir les paramètres par défaut",
        "Retry" to "Réessayer",
        "Run this tool?" to "Exécuter cet outil ?",
        "Save" to "Enregistrer",
        "Save edit" to "Enregistrer la modification",
        "Search" to "Rechercher",
        "Search chats" to "Rechercher des discussions",
        "See also" to "Voir aussi",
        " See the code sample on screen. " to " Consultez l'exemple de code à l'écran. ",
        "Send" to "Envoyer",
        "Send a message" to "Envoyer un message",
        "Set up the Gemma 4 model first." to "Configurez d'abord le modèle Gemma 4.",
        "Settings" to "Paramètres",
        "Speak" to "Parler",
        "Speak whenever you're ready" to "Parlez quand vous êtes prêt",
        "Speaking" to "Parole",
        "Speaking…" to "Parole en cours…",
        "Speech recognition error" to "Erreur de reconnaissance vocale",
        "Speech recognition is busy. Please try again soon." to "La reconnaissance vocale est occupée. Veuillez réessayer bientôt.",
        "Speech recognition is unavailable on this device." to "La reconnaissance vocale n'est pas disponible sur cet appareil.",
        "Speech recognition is unavailable." to "La reconnaissance vocale n'est pas disponible.",
        "Start" to "Démarrer",
        "Start a new chat" to "Démarrer une nouvelle discussion",
        "Start editing" to "Commencer la modification",
        "Start indexing" to "Démarrer l'indexation",
        "Start indexing photos?" to "Démarrer l'indexation des photos ?",
        "Start photo indexing" to "Démarrer l'indexation des photos",
        "Status unknown" to "Statut inconnu",
        "Stop" to "Arrêter",
        "Stop photo indexing" to "Arrêter l'indexation des photos",
        "Stop response" to "Arrêter la réponse",
        "Stopped" to "Arrêté",
        "Stopping" to "Arrêt en cours",
        "Stopping partway keeps the photos already indexed and resumes from there next time." to "L'arrêt en cours de route conserve les photos déjà indexées et reprendra à partir de là la prochaine fois.",
        "Stopping photo indexing" to "Arrêt de l'indexation des photos",
        "Suggest an easy and delicious dinner I can make." to "Suggère-moi un dîner facile et délicieux que je peux préparer.",
        "Suggest one fun idea I could try today." to "Suggère-moi une idée amusante à essayer aujourd'hui.",
        "Supported device list" to "Liste des appareils compatibles",
        "System instructions and pre-prompt" to "Instructions système et pré-invite",
        "Tap again to speak." to "Appuyez à nouveau pour parler.",
        "Tap the microphone to start" to "Appuyez sur le micro pour commencer",
        "Tap to cancel" to "Appuyer pour annuler",
        "Tap to finish" to "Appuyer pour terminer",
        "Tap to speak" to "Appuyer pour parler",
        "Tap to stop" to "Appuyer pour arrêter",
        "Tell me one interesting and useful science or everyday fact." to "Raconte-moi un fait scientifique ou du quotidien intéressant et utile.",
        "Temperature (0.00-1.00)" to "Température (0.00-1.00)",
        "The index was cleared." to "L'index a été effacé.",
        "The model could not be prepared" to "Impossible de préparer le modèle",
        "The model is answering" to "Le modèle répond",
        "The model is downloading." to "Le modèle est en cours de téléchargement.",
        "The model is unavailable on this device" to "Le modèle n'est pas disponible sur cet appareil",
        "The model's thoughts appear here." to "Les pensées du modèle apparaissent ici.",
        "The on-device model is unavailable on this device." to "Le modèle sur l'appareil n'est pas disponible sur cet appareil.",
        "The response was empty. Please try again." to "La réponse était vide. Veuillez réessayer.",
        "The user cancelled the run." to "L'utilisateur a annulé l'exécution.",
        "There is no message to send." to "Aucun message à envoyer.",
        "Thinking" to "Réflexion",
        "Thinking is skipped for voice replies · setting is kept" to "La réflexion est ignorée pour les réponses vocales · le réglage est conservé",
        "Thinking mode" to "Mode réflexion",
        "Thinking stopped" to "Réflexion arrêtée",
        "Thinking unavailable" to "Réflexion non disponible",
        "This response has no thought text to show." to "Cette réponse ne contient aucun texte de réflexion.",
        "This tool and argument combination was already tried once, so it was not retried." to "Cette combinaison d'outil et d'argument a déjà été essayée une fois, elle n'a donc pas été réessayée.",
        "Thought complete" to "Pensée terminée",
        "Tool execution timed out" to "Délai d'exécution de l'outil expiré",
        "Top K (1-128)" to "Top K (1-128)",
        "Trip plan" to "Plan de voyage",
        "Unavailable" to "Non disponible",
        "Unknown" to "Inconnu",
        "Unknown date" to "Date inconnue",
        "User" to "Utilisateur",
        "Using a tool" to "Utilisation d'un outil",
        "Version" to "Version",
        "Vibrates on response start, completion, and tool calls." to "Vibre au début et à la fin de la réponse et aux appels d'outils.",
        "Vibration feedback" to "Retour haptique",
        "Voice chat" to "Chat vocal",
        "Voice chat needs microphone permission." to "Le chat vocal nécessite l'autorisation du microphone.",
        "Voice mode" to "Mode vocal",
        "Warm up model in advance" to "Préchauffer le modèle à l'avance",
        "Wi-Fi is recommended" to "Wi-Fi recommandé",
        "Writing a reply" to "Rédaction d'une réponse",
        "all" to "tout",
        "error: Photo search could not complete because of a local index or on-device embedding error. Do not repeat the same search; tell the user photo search failed." to "erreur : La recherche de photos n'a pas pu aboutir en raison d'une erreur d'index local ou d'intégration sur l'appareil. Ne répétez pas la même recherche ; informez l'utilisateur de l'échec.",
        "error: after cannot be later than before" to "erreur : after ne peut pas être postérieur à before",
        "error: after must be YYYY-MM-DD" to "erreur : after doit être au format AAAA-MM-JJ",
        "error: before must be YYYY-MM-DD" to "erreur : before doit être au format AAAA-MM-JJ",
        "error: could not open an HTTPS connection" to "erreur : impossible d'ouvrir la connexion HTTPS",
        "error: could not resolve the host" to "erreur : impossible de résoudre l'hôte",
        "error: local or private network addresses cannot be fetched" to "erreur : les adresses de réseau local ou privé ne peuvent pas être récupérées",
        "error: only public HTTPS URLs are allowed" to "erreur : seules les URL HTTPS publiques sont autorisées",
        "error: query is required." to "erreur : le terme de recherche est requis.",
        "error: query is required. e.g. search_conversations(query=\"coffee\", limit=5)" to "erreur : query est requis. ex. : search_conversations(query=\"café\", limit=5)",
        "error: query must be 200 characters or fewer" to "erreur : query doit comporter 200 caractères ou moins",
        "error: redirect handling failed" to "erreur : échec du traitement de la redirection",
        "error: the photo search query must be 120 characters or fewer." to "erreur : la requête de recherche de photos doit comporter 120 caractères ou moins.",
        "error: url is required. e.g. web_fetch(url=\"https://example.com\")" to "erreur : url est requise. ex. : web_fetch(url=\"https://example.com\")",
        " link " to " lien ",
    )

    private val MATCHES_REGEX = Regex("""^(\d+) matches$""")
    private val DOWNLOADING_REGEX = Regex("""^Downloading (.+)$""")
    private val SETUP_REGEX = Regex("""^Set up (.+)$""")
    private val USING_REGEX = Regex("""^Using (.+)$""")
    private val TIMED_OUT_REGEX = Regex("""^Timed out \((\d+)s\)$""")
    private val MATCHING_PHOTOS_REGEX = Regex("""^Matching photos \((\d+)\):$""")
    private val INDEX_LOADED_REGEX = Regex("""^Index loaded \((\d+) photos\)$""")
    private val ANALYZING_PHOTOS_PERCENT_REGEX = Regex("""^Analyzing photos \((\d+)%\)$""")
    private val AFTER_REGEX = Regex("""^after (.+)$""")
    private val BEFORE_REGEX = Regex("""^before (.+)$""")
    private val SIMILARITY_REGEX = Regex("""^   Similarity: (\d+)%$""")
    private val TAKEN_REGEX = Regex("""^   Taken: (.+)$""")
    private val MODEL_READY_REGEX = Regex("""^(.+) is ready\.$""")
    private val SPEECH_ERROR_REGEX = Regex("""^Speech recognition is unavailable\. \(code (\d+)\)$""")
    private val UNKNOWN_TOOL_REGEX = Regex("""^Unknown tool '([^']+)'. Available: (.+)$""")
    private val NO_INDEXED_PHOTOS_REGEX = Regex("""^No indexed photos matched '([^']*)'\.$""")
    private val STARTED_ANALYZING_REGEX = Regex("""^Started analyzing photos \((\d+) eligible\)$""")
    private val REDUCED_MAX_OUTPUT_REGEX = Regex("""^Reduced the max output to (\d+) tokens to fit this conversation\. \(setting (\d+)\)$""")
    private val TOOL_CALL_RUNNING_REGEX = Regex("""^Tool call · ([^\n]+)\n([\s\S]*)\nRunning$""")
    private val PHOTO_SEARCH_ERROR_REGEX = Regex("""^error: Photo search failed because the on-device embedding model could not process the query \(error (\d+)\)\. This does not mean there were no matches\. Do not repeat the same search; tell the user photo search failed\.$""")
    private val HTTP_REDIRECT_NO_LOCATION_REGEX = Regex("""^HTTP (\d+) · redirect without a location$""")
    private val HTTP_REDIRECT_LIMIT_EXCEEDED_REGEX = Regex("""^HTTP (\d+) · redirect limit exceeded$""")
    private val HTTP_UNSUPPORTED_CONTENT_TYPE_REGEX = Regex("""^HTTP (\d+) · unsupported content type (.+)$""")
    private val HTTP_COULD_NOT_READ_BODY_REGEX = Regex("""^HTTP (\d+) · could not read the response body$""")

    // Exposes whether the given key exists in the static translation map
    // so the coverage test can distinguish "deliberately kept identical"
    // from "genuinely missing".
    fun containsStaticKey(english: String, language: AppLanguage): Boolean {
        val map = when (language) {
            AppLanguage.JAPANESE -> JA
            AppLanguage.CHINESE -> ZH
            AppLanguage.SPANISH -> ES
            AppLanguage.GERMAN -> DE
            AppLanguage.FRENCH -> FR
            else -> return true
        }
        return map.containsKey(english)
    }

    fun translate(english: String, language: AppLanguage): String {
        val map = when (language) {
            AppLanguage.JAPANESE -> JA
            AppLanguage.CHINESE -> ZH
            AppLanguage.SPANISH -> ES
            AppLanguage.GERMAN -> DE
            AppLanguage.FRENCH -> FR
            else -> return english
        }

        // 1. Direct O(1) map lookup
        map[english]?.let { return it }

        // 2. Dynamic parameterized pattern lookups
        MATCHES_REGEX.matchEntire(english)?.let { m ->
            val count = m.groupValues[1]
            return when (language) {
                AppLanguage.JAPANESE -> "一致 ${count}件"
                AppLanguage.CHINESE -> "匹配 $count 条"
                AppLanguage.SPANISH -> "$count coincidencias"
                AppLanguage.GERMAN -> "$count Treffer"
                AppLanguage.FRENCH -> "$count correspondances"
                else -> english
            }
        }

        DOWNLOADING_REGEX.matchEntire(english)?.let { m ->
            val model = m.groupValues[1]
            return when (language) {
                AppLanguage.JAPANESE -> "${model}をダウンロード中"
                AppLanguage.CHINESE -> "正在下载 $model"
                AppLanguage.SPANISH -> "Descargando $model"
                AppLanguage.GERMAN -> "$model wird heruntergeladen"
                AppLanguage.FRENCH -> "Téléchargement de $model"
                else -> english
            }
        }

        SETUP_REGEX.matchEntire(english)?.let { m ->
            val model = m.groupValues[1]
            return when (language) {
                AppLanguage.JAPANESE -> "${model}の準備"
                AppLanguage.CHINESE -> "准备 $model"
                AppLanguage.SPANISH -> "Configurar $model"
                AppLanguage.GERMAN -> "$model einrichten"
                AppLanguage.FRENCH -> "Configurer $model"
                else -> english
            }
        }

        USING_REGEX.matchEntire(english)?.let { m ->
            val tool = m.groupValues[1]
            return when (language) {
                AppLanguage.JAPANESE -> "${tool}を使用中"
                AppLanguage.CHINESE -> "正在使用 $tool"
                AppLanguage.SPANISH -> "Usando $tool"
                AppLanguage.GERMAN -> "$tool wird verwendet"
                AppLanguage.FRENCH -> "Utilisation de $tool"
                else -> english
            }
        }

        TIMED_OUT_REGEX.matchEntire(english)?.let { m ->
            val sec = m.groupValues[1]
            return when (language) {
                AppLanguage.JAPANESE -> "タイムアウト(${sec}秒)"
                AppLanguage.CHINESE -> "超时 (${sec}秒)"
                AppLanguage.SPANISH -> "Tiempo agotado (${sec}s)"
                AppLanguage.GERMAN -> "Zeitüberschreitung (${sec}s)"
                AppLanguage.FRENCH -> "Délai expiré (${sec}s)"
                else -> english
            }
        }

        MATCHING_PHOTOS_REGEX.matchEntire(english)?.let { m ->
            val count = m.groupValues[1]
            return when (language) {
                AppLanguage.JAPANESE -> "一致した写真 (${count}枚):"
                AppLanguage.CHINESE -> "匹配的照片 (${count}张):"
                AppLanguage.SPANISH -> "Fotos coincidentes ($count):"
                AppLanguage.GERMAN -> "Passende Fotos ($count):"
                AppLanguage.FRENCH -> "Photos correspondantes ($count):"
                else -> english
            }
        }

        INDEX_LOADED_REGEX.matchEntire(english)?.let { m ->
            val count = m.groupValues[1]
            return when (language) {
                AppLanguage.JAPANESE -> "インデックス読み込み完了 (${count}枚)"
                AppLanguage.CHINESE -> "索引加载完成 (${count}张)"
                AppLanguage.SPANISH -> "Índice cargado ($count fotos)"
                AppLanguage.GERMAN -> "Index geladen ($count Fotos)"
                AppLanguage.FRENCH -> "Index chargé ($count photos)"
                else -> english
            }
        }

        ANALYZING_PHOTOS_PERCENT_REGEX.matchEntire(english)?.let { m ->
            val pct = m.groupValues[1]
            return when (language) {
                AppLanguage.JAPANESE -> "写真を分析中 ($pct%)"
                AppLanguage.CHINESE -> "正在分析照片 ($pct%)"
                AppLanguage.SPANISH -> "Analizando fotos ($pct%)"
                AppLanguage.GERMAN -> "Fotos werden analysiert ($pct%)"
                AppLanguage.FRENCH -> "Analyse des photos ($pct%)"
                else -> english
            }
        }

        AFTER_REGEX.matchEntire(english)?.let { m ->
            val date = m.groupValues[1]
            return when (language) {
                AppLanguage.JAPANESE -> "${date}以降"
                AppLanguage.CHINESE -> "$date 之后"
                AppLanguage.SPANISH -> "después de $date"
                AppLanguage.GERMAN -> "nach $date"
                AppLanguage.FRENCH -> "après $date"
                else -> english
            }
        }

        BEFORE_REGEX.matchEntire(english)?.let { m ->
            val date = m.groupValues[1]
            return when (language) {
                AppLanguage.JAPANESE -> "${date}以前"
                AppLanguage.CHINESE -> "$date 之前"
                AppLanguage.SPANISH -> "antes de $date"
                AppLanguage.GERMAN -> "vor $date"
                AppLanguage.FRENCH -> "avant $date"
                else -> english
            }
        }

        SIMILARITY_REGEX.matchEntire(english)?.let { m ->
            val score = m.groupValues[1]
            return when (language) {
                AppLanguage.JAPANESE -> "   類似度: ${score}%"
                AppLanguage.CHINESE -> "   相似度: ${score}%"
                AppLanguage.SPANISH -> "   Similitud: ${score}%"
                AppLanguage.GERMAN -> "   Ähnlichkeit: ${score}%"
                AppLanguage.FRENCH -> "   Similarité: ${score}%"
                else -> english
            }
        }

        TAKEN_REGEX.matchEntire(english)?.let { m ->
            val date = m.groupValues[1]
            return when (language) {
                AppLanguage.JAPANESE -> "   撮影日時: $date"
                AppLanguage.CHINESE -> "   拍摄时间: $date"
                AppLanguage.SPANISH -> "   Fecha de captura: $date"
                AppLanguage.GERMAN -> "   Aufnahmedatum: $date"
                AppLanguage.FRENCH -> "   Date de prise de vue: $date"
                else -> english
            }
        }

        MODEL_READY_REGEX.matchEntire(english)?.let { m ->
            val model = m.groupValues[1]
            return when (language) {
                AppLanguage.JAPANESE -> "${model}の準備が完了しました。"
                AppLanguage.CHINESE -> "$model 准备完成。"
                AppLanguage.SPANISH -> "$model está listo."
                AppLanguage.GERMAN -> "$model ist bereit."
                AppLanguage.FRENCH -> "$model est prêt."
                else -> english
            }
        }

        SPEECH_ERROR_REGEX.matchEntire(english)?.let { m ->
            val code = m.groupValues[1]
            return when (language) {
                AppLanguage.JAPANESE -> "音声認識を利用できません。(コード $code)"
                AppLanguage.CHINESE -> "语音识别不可用。(代码 $code)"
                AppLanguage.SPANISH -> "El reconocimiento de voz no está disponible. (código $code)"
                AppLanguage.GERMAN -> "Spracherkennung ist nicht verfügbar. (Code $code)"
                AppLanguage.FRENCH -> "La reconnaissance vocale n'est pas disponible. (code $code)"
                else -> english
            }
        }

        UNKNOWN_TOOL_REGEX.matchEntire(english)?.let { m ->
            val tool = m.groupValues[1]
            val avail = m.groupValues[2]
            return when (language) {
                AppLanguage.JAPANESE -> "不明なツール '$tool'。利用可能: $avail"
                AppLanguage.CHINESE -> "未知工具 '$tool'。可用工具: $avail"
                AppLanguage.SPANISH -> "Herramienta desconocida '$tool'. Disponibles: $avail"
                AppLanguage.GERMAN -> "Unbekanntes Tool '$tool'. Verfügbar: $avail"
                AppLanguage.FRENCH -> "Outil inconnu '$tool'. Disponible : $avail"
                else -> english
            }
        }

        NO_INDEXED_PHOTOS_REGEX.matchEntire(english)?.let { m ->
            val query = m.groupValues[1]
            return when (language) {
                AppLanguage.JAPANESE -> "'$query'に一致するインデックス写真は見つかりませんでした。"
                AppLanguage.CHINESE -> "未找到与 '$query' 匹配的已索引照片。"
                AppLanguage.SPANISH -> "No se encontraron fotos indexadas que coincidan con '$query'."
                AppLanguage.GERMAN -> "Keine indizierten Fotos gefunden, die '$query' entsprechen."
                AppLanguage.FRENCH -> "Aucune photo indexée ne correspond à '$query'."
                else -> english
            }
        }

        STARTED_ANALYZING_REGEX.matchEntire(english)?.let { m ->
            val total = m.groupValues[1]
            return when (language) {
                AppLanguage.JAPANESE -> "写真分析を開始 (対象 ${total}枚)"
                AppLanguage.CHINESE -> "开始分析照片 (符合条件 ${total}张)"
                AppLanguage.SPANISH -> "Se inició el análisis de fotos ($total aptas)"
                AppLanguage.GERMAN -> "Fotoanalyse gestartet ($total berechtigt)"
                AppLanguage.FRENCH -> "Analyse des photos démarrée ($total éligibles)"
                else -> english
            }
        }

        REDUCED_MAX_OUTPUT_REGEX.matchEntire(english)?.let { m ->
            val eff = m.groupValues[1]
            val req = m.groupValues[2]
            return when (language) {
                AppLanguage.JAPANESE -> "会話に合わせて最大出力を ${eff}トークンに縮小しました。(設定値 $req)"
                AppLanguage.CHINESE -> "为适应当前对话长度，已将最大输出下调至 ${eff} Token (设定值 $req)"
                AppLanguage.SPANISH -> "Se redujo la salida máxima a $eff tokens para ajustarse a esta conversación. (ajuste $req)"
                AppLanguage.GERMAN -> "Die maximale Ausgabe wurde auf $eff Token reduziert, um zur Unterhaltung zu passen. (Einstellung $req)"
                AppLanguage.FRENCH -> "Sortie maximale réduite à $eff jetons pour cette conversation. (paramètre $req)"
                else -> english
            }
        }

        TOOL_CALL_RUNNING_REGEX.matchEntire(english)?.let { m ->
            val name = m.groupValues[1]
            val args = m.groupValues[2]
            return when (language) {
                AppLanguage.JAPANESE -> "ツール呼び出し · $name\n$args\n実行中"
                AppLanguage.CHINESE -> "工具调用 · $name\n$args\n运行中"
                AppLanguage.SPANISH -> "Llamada a herramienta · $name\n$args\nEn ejecución"
                AppLanguage.GERMAN -> "Tool-Aufruf · $name\n$args\nWird ausgeführt"
                AppLanguage.FRENCH -> "Appel d'outil · $name\n$args\nEn cours d'exécution"
                else -> english
            }
        }

        PHOTO_SEARCH_ERROR_REGEX.matchEntire(english)?.let { m ->
            val errCode = m.groupValues[1]
            return when (language) {
                AppLanguage.JAPANESE -> "エラー: オンデバイス埋め込みモデルが検索クエリを処理できなかったため、写真検索に失敗しました(エラー $errCode)。一致する写真がないという意味ではありません。同じ検索を繰り返さず、検索失敗をユーザーに通知してください。"
                AppLanguage.CHINESE -> "错误：端侧向量模型无法处理搜索请求，照片搜索失败(错误代码 $errCode)。这并不意味着没有匹配的照片。请勿重复同一搜索，直接告知用户搜索失败。"
                AppLanguage.SPANISH -> "error: La búsqueda de fotos falló porque el modelo de incrustación del dispositivo no pudo procesar la consulta (error $errCode). Esto no significa que no hubiera coincidencias. No repitas la misma búsqueda; informa al usuario del fallo."
                AppLanguage.GERMAN -> "Fehler: Die Fotosuche ist fehlgeschlagen, da das On-Device-Einbettungsmodell die Anfrage nicht verarbeiten konnte (Fehler $errCode). Dies bedeutet nicht, dass es keine Treffer gab. Wiederhole die Suche nicht; informiere den Benutzer über den Fehler."
                AppLanguage.FRENCH -> "erreur : La recherche de photos a échoué car le modèle d'intégration de l'appareil n'a pas pu traiter la requête (erreur $errCode). Cela ne signifie pas qu'il n'y avait aucune correspondance. Ne répétez pas la même recherche ; informez l'utilisateur de l'échec."
                else -> english
            }
        }

        HTTP_REDIRECT_NO_LOCATION_REGEX.matchEntire(english)?.let { m ->
            val status = m.groupValues[1]
            return when (language) {
                AppLanguage.JAPANESE -> "HTTP $status · リダイレクト先がありません"
                AppLanguage.CHINESE -> "HTTP $status · 重定向未包含目标地址"
                AppLanguage.SPANISH -> "HTTP $status · redirección sin ubicación"
                AppLanguage.GERMAN -> "HTTP $status · Weiterleitung ohne Ziel"
                AppLanguage.FRENCH -> "HTTP $status · redirection sans adresse"
                else -> english
            }
        }

        HTTP_REDIRECT_LIMIT_EXCEEDED_REGEX.matchEntire(english)?.let { m ->
            val status = m.groupValues[1]
            return when (language) {
                AppLanguage.JAPANESE -> "HTTP $status · リダイレクト回数上限を超えました"
                AppLanguage.CHINESE -> "HTTP $status · 超过重定向次数限制"
                AppLanguage.SPANISH -> "HTTP $status · se superó el límite de redirecciones"
                AppLanguage.GERMAN -> "HTTP $status · Weiterleitungslimit überschritten"
                AppLanguage.FRENCH -> "HTTP $status · limite de redirections dépassée"
                else -> english
            }
        }

        HTTP_UNSUPPORTED_CONTENT_TYPE_REGEX.matchEntire(english)?.let { m ->
            val status = m.groupValues[1]
            val contentType = m.groupValues[2]
            return when (language) {
                AppLanguage.JAPANESE -> "HTTP $status · 未対応のコンテンツ形式: $contentType"
                AppLanguage.CHINESE -> "HTTP $status · 不支持的内容格式: $contentType"
                AppLanguage.SPANISH -> "HTTP $status · tipo de contenido no admitido: $contentType"
                AppLanguage.GERMAN -> "HTTP $status · nicht unterstützter Inhaltstyp: $contentType"
                AppLanguage.FRENCH -> "HTTP $status · type de contenu non pris en charge : $contentType"
                else -> english
            }
        }

        HTTP_COULD_NOT_READ_BODY_REGEX.matchEntire(english)?.let { m ->
            val status = m.groupValues[1]
            return when (language) {
                AppLanguage.JAPANESE -> "HTTP $status · レスポンス本文を読み取れませんでした"
                AppLanguage.CHINESE -> "HTTP $status · 无法读取响应正文"
                AppLanguage.SPANISH -> "HTTP $status · no se pudo leer el cuerpo de la respuesta"
                AppLanguage.GERMAN -> "HTTP $status · Antworttext konnte nicht gelesen werden"
                AppLanguage.FRENCH -> "HTTP $status · impossible de lire le corps de la réponse"
                else -> english
            }
        }

        return english
    }
}
