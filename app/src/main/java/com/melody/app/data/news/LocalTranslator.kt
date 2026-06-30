package com.melody.app.data.news

/**
 * 本地 AI 术语翻译字典
 * 替代 MLKit（需连 Google 服务器，国内不可达）
 */
object LocalTranslator {

    private val termMap = linkedMapOf(
        // AI 核心术语（长词组优先）
        "large language model" to "大语言模型",
        "artificial intelligence" to "人工智能",
        "machine learning" to "机器学习",
        "deep learning" to "深度学习",
        "natural language processing" to "自然语言处理",
        "computer vision" to "计算机视觉",
        "reinforcement learning" to "强化学习",
        "generative ai" to "生成式AI",
        "foundation model" to "基础模型",
        "neural network" to "神经网络",
        "knowledge distillation" to "知识蒸馏",
        "retrieval-augmented" to "检索增强",
        "retrieval augmented" to "检索增强",
        "text-to-image" to "文生图",
        "text-to-video" to "文生视频",
        "speech recognition" to "语音识别",
        "object detection" to "目标检测",
        "sentiment analysis" to "情感分析",
        "chain-of-thought" to "思维链",
        "transfer learning" to "迁移学习",
        "contrastive learning" to "对比学习",
        "federated learning" to "联邦学习",
        "differential privacy" to "差分隐私",
        "feature engineering" to "特征工程",
        "data augmentation" to "数据增强",
        "knowledge graph" to "知识图谱",
        "edge deployment" to "边缘部署",
        "open-source" to "开源",
        "open source" to "开源",
        "closed-source" to "闭源",
        "state-of-the-art" to "最先进",
        "in-context learning" to "上下文学习",
        "prompt engineering" to "提示词工程",
        "fine-tuning" to "微调",
        "fine-tune" to "微调",
        "fine tuning" to "微调",
        "self-supervised" to "自监督",
        "semi-supervised" to "半监督",
        "backpropagation" to "反向传播",
        "gradient descent" to "梯度下降",
        "loss function" to "损失函数",
        "activation function" to "激活函数",
        "batch normalization" to "批量归一化",
        "layer normalization" to "层归一化",
        "residual connection" to "残差连接",
        "autoencoder" to "自编码器",
        "generative adversarial" to "生成对抗",
        "diffusion model" to "扩散模型",
        "convolutional" to "卷积",
        "recurrent" to "循环",
        "tokenizer" to "分词器",
        "pre-training" to "预训练",
        "pre-trained" to "预训练",
        "model compression" to "模型压缩",
        "hyperparameter" to "超参数",
        "learning rate" to "学习率",
        "batch size" to "批大小",
        "quantization" to "量化",
        "pruning" to "剪枝",
        "distillation" to "蒸馏",
        // AI 产品/公司
        "openai" to "OpenAI",
        "anthropic" to "Anthropic",
        "google deepmind" to "Google DeepMind",
        "meta ai" to "Meta AI",
        "hugging face" to "Hugging Face",
        "huggingface" to "Hugging Face",
        "stability ai" to "Stability AI",
        "midjourney" to "Midjourney",
        "copilot" to "Copilot",
        "chatgpt" to "ChatGPT",
        "gemini" to "Gemini",
        "claude" to "Claude",
        "llama" to "Llama",
        "dall-e" to "DALL-E",
        "nvidia" to "英伟达",
        "microsoft" to "微软",
        "amazon" to "亚马逊",
        "apple" to "苹果",
        "baidu" to "百度",
        "alibaba" to "阿里巴巴",
        "tencent" to "腾讯",
        "bytedance" to "字节跳动",
        "mistral" to "Mistral AI",
        // AI 概念
        "multimodal" to "多模态",
        "reasoning" to "推理",
        "hallucination" to "幻觉",
        "alignment" to "对齐",
        "guardrail" to "护栏",
        "benchmark" to "基准测试",
        "leaderboard" to "排行榜",
        "embedding" to "嵌入",
        "vector" to "向量",
        "tokenizer" to "分词器",
        "corpus" to "语料库",
        "annotation" to "标注",
        "labeling" to "标注",
        "dataset" to "数据集",
        "inference" to "推理",
        "training" to "训练",
        "deployment" to "部署",
        "api" to "API",
        "sdk" to "SDK",
        "framework" to "框架",
        "library" to "库",
        "model" to "模型",
        "agent" to "智能体",
        "chatbot" to "聊天机器人",
        "virtual assistant" to "虚拟助手",
        "autonomous" to "自主",
        "robotics" to "机器人技术",
        "synthetic data" to "合成数据",
        "zero-shot" to "零样本",
        "few-shot" to "少样本",
        "encoder" to "编码器",
        "decoder" to "解码器",
        "parameter" to "参数",
        "epoch" to "训练轮次",
        "accuracy" to "准确率",
        "precision" to "精确率",
        "recall" to "召回率",
        "supervised" to "监督式",
        "unsupervised" to "无监督",
        "classification" to "分类",
        "regression" to "回归",
        "clustering" to "聚类",
        "optimization" to "优化",
        "regularization" to "正则化",
        "dropout" to "随机失活",
        "scalability" to "可扩展性",
        "latency" to "延迟",
        "throughput" to "吞吐量",
        "real-time" to "实时",
        "on-device" to "端侧",
        "edge" to "端侧",
        "cloud" to "云端",
        // 新闻常见动词
        "announces" to "宣布",
        "announce" to "宣布",
        "introduces" to "推出",
        "introduce" to "推出",
        "unveils" to "发布",
        "unveil" to "发布",
        "launches" to "发布",
        "launch" to "发布",
        "releases" to "发布",
        "release" to "发布",
        "raises" to "融资",
        "acquires" to "收购",
        "acquire" to "收购",
        "partners" to "合作",
        "expands" to "扩展",
        "expand" to "扩展",
        "improves" to "改进",
        "improve" to "改进",
        "exceeds" to "超过",
        "surpasses" to "超越",
        "surges" to "激增",
        "drops" to "下跌",
        "rises" to "上涨",
        "grows" to "增长",
        "declines" to "下降",
        "reveals" to "揭示",
        "reveal" to "揭示",
        "exposes" to "暴露",
        "warns" to "警告",
        "warn" to "警告",
        "claims" to "声称",
        "suggests" to "表明",
        "confirms" to "确认",
        "confirm" to "确认",
        "denies" to "否认",
        "deny" to "否认",
        "predicts" to "预测",
        "predict" to "预测",
        "targets" to "目标",
        "addresses" to "解决",
        "challenges" to "挑战",
        "threatens" to "威胁",
        "competes" to "竞争",
        "dominates" to "主导",
        "leads" to "引领",
        "drives" to "驱动",
        "powers" to "驱动",
        "enables" to "支持",
        "supports" to "支持",
        "requires" to "需要",
        "demands" to "要求",
        "affects" to "影响",
        "impacts" to "影响",
        "transforms" to "变革",
        "revolutionizes" to "革命性改变",
        "disrupts" to "颠覆",
        "reshapes" to "重塑",
        "defines" to "定义",
        "redefines" to "重新定义"
    )

    /**
     * 翻译文本中的英文术语
     */
    fun translateTerms(text: String): String {
        if (text.isBlank()) return text
        var result = text
        for ((en, cn) in termMap) {
            result = result.replace(Regex("\\b${Regex.escape(en)}\\b", RegexOption.IGNORE_CASE), cn)
        }
        return result.replace(Regex("\\s+"), " ").trim()
    }

    /**
     * 判断文本是否主要是英文
     */
    fun isMostlyEnglish(text: String): Boolean {
        if (text.isBlank()) return false
        val latin = text.count { it in 'a'..'z' || it in 'A'..'Z' }
        val cjk = text.count { it in '\u4e00'..'\u9fff' }
        return latin > cjk * 3 && latin > 10
    }

    /**
     * 翻译英文文本（术语替换 + 句式翻译）
     * 从第一性原理：先翻译术语，再用句式规则重组句子
     */
    fun translateEnglish(text: String): String {
        if (!isMostlyEnglish(text)) return text
        var result = translateTerms(text)

        // 句式翻译：新闻标题常见模式
        // 模式1: "X announces/launches/releases Y" → "X 发布 Y"
        result = result.replace(
            Regex("^(\\S+(?:\\s\\S+)?)\\s+(?:宣布|推出|发布)\\s+(.+)$"),
            "$1 $2"
        )

        // 模式2: "X raises Y million in funding" → "X 融资 Y 百万"
        result = result.replace(
            Regex("^(\\S+(?:\\s\\S+)?)\\s+融资\\s+(\\d[\\d,.]*)\\s*(?:百万|十亿|亿)\\s*(?:in\\s+)?(?:融资|投资)?"),
            "$1 融资$2"
        )

        // 模式3: "How X is changing Y" → "X 如何改变 Y"
        result = result.replace(
            Regex("^How\\s+(\\S+(?:\\s\\S+)?)\\s+is\\s+(.+)$", RegexOption.IGNORE_CASE),
            "$1 如何$2"
        )

        // 模式4: "X vs Y: ..." → "X 与 Y 对比：..."
        result = result.replace(
            Regex("^(\\S+)\\s+vs\\.?\\s+(\\S+):?\\s*(.+)$", RegexOption.IGNORE_CASE),
            "$1 与 $2 对比：$3"
        )

        // 模式5: "Why X matters" → "为什么 X 很重要"
        result = result.replace(
            Regex("^Why\\s+(.+)\\s+matters?$", RegexOption.IGNORE_CASE),
            "为什么 $1 很重要"
        )

        // 模式6: "The future of X" → "X 的未来"
        result = result.replace(
            Regex("^The\\s+future\\s+of\\s+(.+)$", RegexOption.IGNORE_CASE),
            "$1 的未来"
        )

        // 模式7: "X is the new Y" → "X 成为新的 Y"
        result = result.replace(
            Regex("^(.+)\\s+is\\s+the\\s+new\\s+(.+)$", RegexOption.IGNORE_CASE),
            "$1 成为新的 $2"
        )

        // 模式8: "Top N X" → "十大 X"
        result = result.replace(
            Regex("^Top\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE),
            "前$1 $2"
        )

        // 模式9: "X: Y" → "X：Y"（冒号前后加空格）
        result = result.replace(Regex("\\s*:\\s*"), "：")

        // 模式10: 清理常见无意义词
        result = result
            .replace(Regex("\\bthe\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\ban?\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\bis\\b", RegexOption.IGNORE_CASE), "是")
            .replace(Regex("\\bare\\b", RegexOption.IGNORE_CASE), "是")
            .replace(Regex("\\bwas\\b", RegexOption.IGNORE_CASE), "曾")
            .replace(Regex("\\bhas\\b", RegexOption.IGNORE_CASE), "已")
            .replace(Regex("\\bhave\\b", RegexOption.IGNORE_CASE), "已")
            .replace(Regex("\\bwill\\b", RegexOption.IGNORE_CASE), "将")
            .replace(Regex("\\bcan\\b", RegexOption.IGNORE_CASE), "可")
            .replace(Regex("\\bwith\\b", RegexOption.IGNORE_CASE), "与")
            .replace(Regex("\\bfor\\b", RegexOption.IGNORE_CASE), "为")
            .replace(Regex("\\band\\b", RegexOption.IGNORE_CASE), "和")
            .replace(Regex("\\bor\\b", RegexOption.IGNORE_CASE), "或")
            .replace(Regex("\\bbut\\b", RegexOption.IGNORE_CASE), "但")
            .replace(Regex("\\bin\\b", RegexOption.IGNORE_CASE), "在")
            .replace(Regex("\\bof\\b", RegexOption.IGNORE_CASE), "的")
            .replace(Regex("\\bto\\b", RegexOption.IGNORE_CASE), "到")
            .replace(Regex("\\bnew\\b", RegexOption.IGNORE_CASE), "新")
            .replace(Regex("\\bfree\\b", RegexOption.IGNORE_CASE), "免费")
            .replace(Regex("\\bbig\\b", RegexOption.IGNORE_CASE), "大")
            .replace(Regex("\\bopen\\b", RegexOption.IGNORE_CASE), "开放")
            .replace(Regex("\\bfirst\\b", RegexOption.IGNORE_CASE), "首个")
            .replace(Regex("\\blaunch(es|ed)?\\b", RegexOption.IGNORE_CASE), "发布")
            .replace(Regex("\\brelease[ds]?\\b", RegexOption.IGNORE_CASE), "发布")
            .replace(Regex("\\bannounces?\\b", RegexOption.IGNORE_CASE), "宣布")
            .replace(Regex("\\bintroduces?\\b", RegexOption.IGNORE_CASE), "推出")
            .replace(Regex("\\bunveils?\\b", RegexOption.IGNORE_CASE), "推出")
            .replace(Regex("\\braises?\\b", RegexOption.IGNORE_CASE), "融资")
            .replace(Regex("\\bbillion\\b", RegexOption.IGNORE_CASE), "十亿")
            .replace(Regex("\\bmillion\\b", RegexOption.IGNORE_CASE), "百万")
            .replace(Regex("\\bstartup\\b", RegexOption.IGNORE_CASE), "初创公司")
            .replace(Regex("\\bcompany\\b", RegexOption.IGNORE_CASE), "公司")
            .replace(Regex("\\bresearch\\b", RegexOption.IGNORE_CASE), "研究")
            .replace(Regex("\\bprivacy\\b", RegexOption.IGNORE_CASE), "隐私")
            .replace(Regex("\\bregulation\\b", RegexOption.IGNORE_CASE), "监管")
            .replace(Regex("\\bsafety\\b", RegexOption.IGNORE_CASE), "安全")
            .replace(Regex("\\busers?\\b", RegexOption.IGNORE_CASE), "用户")
            .replace(Regex("\\btools?\\b", RegexOption.IGNORE_CASE), "工具")
            .replace(Regex("\\bapps?\\b", RegexOption.IGNORE_CASE), "应用")
            .replace(Regex("\\bchatbot\\b", RegexOption.IGNORE_CASE), "聊天机器人")
            .replace(Regex("\\bimage\\b", RegexOption.IGNORE_CASE), "图像")
            .replace(Regex("\\bvideo\\b", RegexOption.IGNORE_CASE), "视频")
            .replace(Regex("\\baudio\\b", RegexOption.IGNORE_CASE), "音频")
            .replace(Regex("\\btext\\b", RegexOption.IGNORE_CASE), "文本")
            .replace(Regex("\\bcode\\b", RegexOption.IGNORE_CASE), "代码")
            .replace(Regex("\\bcoding\\b", RegexOption.IGNORE_CASE), "编程")
            .replace(Regex("\\bdeveloper\\b", RegexOption.IGNORE_CASE), "开发者")
            .replace(Regex("\\bcompanies\\b", RegexOption.IGNORE_CASE), "公司")
            .replace(Regex("\\bbusiness\\b", RegexOption.IGNORE_CASE), "商业")
            .replace(Regex("\\bmarket\\b", RegexOption.IGNORE_CASE), "市场")
            .replace(Regex("\\bindustry\\b", RegexOption.IGNORE_CASE), "行业")
            .replace(Regex("\\bgovernment\\b", RegexOption.IGNORE_CASE), "政府")
            .replace(Regex("\\bfuture\\b", RegexOption.IGNORE_CASE), "未来")
            .replace(Regex("\\btrend\\b", RegexOption.IGNORE_CASE), "趋势")
            .replace(Regex("\\bgrowth\\b", RegexOption.IGNORE_CASE), "增长")
            .replace(Regex("\\bchallenge\\b", RegexOption.IGNORE_CASE), "挑战")
            .replace(Regex("\\bopportunity\\b", RegexOption.IGNORE_CASE), "机遇")
            .replace(Regex("\\brisk\\b", RegexOption.IGNORE_CASE), "风险")
            .replace(Regex("\\bvalue\\b", RegexOption.IGNORE_CASE), "价值")
            .replace(Regex("\\bimpact\\b", RegexOption.IGNORE_CASE), "影响")
            .replace(Regex("\\bchange\\b", RegexOption.IGNORE_CASE), "变化")
            .replace(Regex("\\bbuild\\b", RegexOption.IGNORE_CASE), "构建")
            .replace(Regex("\\bcreate\\b", RegexOption.IGNORE_CASE), "创建")
            .replace(Regex("\\bdevelop\\b", RegexOption.IGNORE_CASE), "开发")
            .replace(Regex("\\bdesign\\b", RegexOption.IGNORE_CASE), "设计")
            .replace(Regex("\\btest\\b", RegexOption.IGNORE_CASE), "测试")
            .replace(Regex("\\bimprove\\b", RegexOption.IGNORE_CASE), "改进")
            .replace(Regex("\\benhance\\b", RegexOption.IGNORE_CASE), "增强")
            .replace(Regex("\\boptimize\\b", RegexOption.IGNORE_CASE), "优化")
            .replace(Regex("\\breduce\\b", RegexOption.IGNORE_CASE), "减少")
            .replace(Regex("\\bincrease\\b", RegexOption.IGNORE_CASE), "增加")
            .replace(Regex("\\bexpand\\b", RegexOption.IGNORE_CASE), "扩展")
            .replace(Regex("\\bscale\\b", RegexOption.IGNORE_CASE), "规模化")
            .replace(Regex("\\bsolve\\b", RegexOption.IGNORE_CASE), "解决")
            .replace(Regex("\\bfix\\b", RegexOption.IGNORE_CASE), "修复")
            .replace(Regex("\\bneed\\b", RegexOption.IGNORE_CASE), "需要")
            .replace(Regex("\\bwant\\b", RegexOption.IGNORE_CASE), "希望")
            .replace(Regex("\\bsay\\b", RegexOption.IGNORE_CASE), "表示")
            .replace(Regex("\\breports?\\b", RegexOption.IGNORE_CASE), "报道")
            .replace(Regex("\\bshow\\b", RegexOption.IGNORE_CASE), "显示")
            .replace(Regex("\\breveal\\b", RegexOption.IGNORE_CASE), "揭示")
            .replace(Regex("\\bdemonstrate\\b", RegexOption.IGNORE_CASE), "证明")
            .replace(Regex("\\bwarn\\b", RegexOption.IGNORE_CASE), "警告")
            .replace(Regex("\\bthreat\\b", RegexOption.IGNORE_CASE), "威胁")
            .replace(Regex("\\bsecure\\b", RegexOption.IGNORE_CASE), "安全")
            .replace(Regex("\\bprotect\\b", RegexOption.IGNORE_CASE), "保护")
            .replace(Regex("\\bpublic\\b", RegexOption.IGNORE_CASE), "公开")
            .replace(Regex("\\bprivate\\b", RegexOption.IGNORE_CASE), "私有")
            .replace(Regex("\\bimportant\\b", RegexOption.IGNORE_CASE), "重要")
            .replace(Regex("\\bcritical\\b", RegexOption.IGNORE_CASE), "关键")
            .replace(Regex("\\bnecessary\\b", RegexOption.IGNORE_CASE), "必要")
            .replace(Regex("\\bpossible\\b", RegexOption.IGNORE_CASE), "可能")
            .replace(Regex("\\bimpossible\\b", RegexOption.IGNORE_CASE), "不可能")
            .replace(Regex("\\blikely\\b", RegexOption.IGNORE_CASE), "可能")
            .replace(Regex("\\bcertain\\b", RegexOption.IGNORE_CASE), "确定")
            .replace(Regex("\\bclear\\b", RegexOption.IGNORE_CASE), "明确")
            .replace(Regex("\\bnow\\b", RegexOption.IGNORE_CASE), "现在")
            .replace(Regex("\\btoday\\b", RegexOption.IGNORE_CASE), "今天")
            .replace(Regex("\\brecently\\b", RegexOption.IGNORE_CASE), "最近")
            .replace(Regex("\\balready\\b", RegexOption.IGNORE_CASE), "已经")
            .replace(Regex("\\bstill\\b", RegexOption.IGNORE_CASE), "仍然")
            .replace(Regex("\\balso\\b", RegexOption.IGNORE_CASE), "也")
            .replace(Regex("\\bjust\\b", RegexOption.IGNORE_CASE), "刚刚")
            .replace(Regex("\\bonly\\b", RegexOption.IGNORE_CASE), "仅")
            .replace(Regex("\\bmore\\b", RegexOption.IGNORE_CASE), "更多")
            .replace(Regex("\\bless\\b", RegexOption.IGNORE_CASE), "更少")
            .replace(Regex("\\bbetter\\b", RegexOption.IGNORE_CASE), "更好")
            .replace(Regex("\\bworse\\b", RegexOption.IGNORE_CASE), "更差")
            .replace(Regex("\\bfaster\\b", RegexOption.IGNORE_CASE), "更快")
            .replace(Regex("\\bslower\\b", RegexOption.IGNORE_CASE), "更慢")
            .replace(Regex("\\blarger\\b", RegexOption.IGNORE_CASE), "更大")
            .replace(Regex("\\bsmaller\\b", RegexOption.IGNORE_CASE), "更小")
            .replace(Regex("\\bhigher\\b", RegexOption.IGNORE_CASE), "更高")
            .replace(Regex("\\blower\\b", RegexOption.IGNORE_CASE), "更低")
            .replace(Regex("\\bmodern\\b", RegexOption.IGNORE_CASE), "现代")
            .replace(Regex("\\btraditional\\b", RegexOption.IGNORE_CASE), "传统")
            .replace(Regex("\\bbasic\\b", RegexOption.IGNORE_CASE), "基础")
            .replace(Regex("\\badvanced\\b", RegexOption.IGNORE_CASE), "高级")
            .replace(Regex("\\bcomplex\\b", RegexOption.IGNORE_CASE), "复杂")
            .replace(Regex("\\bsimple\\b", RegexOption.IGNORE_CASE), "简单")
            .replace(Regex("\\beasy\\b", RegexOption.IGNORE_CASE), "容易")
            .replace(Regex("\\bhard\\b", RegexOption.IGNORE_CASE), "困难")
            .replace(Regex("\\bdifficult\\b", RegexOption.IGNORE_CASE), "困难")
            .replace(Regex("\\bfast\\b", RegexOption.IGNORE_CASE), "快速")
            .replace(Regex("\\bslow\\b", RegexOption.IGNORE_CASE), "缓慢")
            .replace(Regex("\\bbig\\b", RegexOption.IGNORE_CASE), "大")
            .replace(Regex("\\bsmall\\b", RegexOption.IGNORE_CASE), "小")
            .replace(Regex("\\blong\\b", RegexOption.IGNORE_CASE), "长")
            .replace(Regex("\\bshort\\b", RegexOption.IGNORE_CASE), "短")
            .replace(Regex("\\bhigh\\b", RegexOption.IGNORE_CASE), "高")
            .replace(Regex("\\blow\\b", RegexOption.IGNORE_CASE), "低")
            .replace(Regex("\\bgood\\b", RegexOption.IGNORE_CASE), "好")
            .replace(Regex("\\bbad\\b", RegexOption.IGNORE_CASE), "坏")
            .replace(Regex("\\bright\\b", RegexOption.IGNORE_CASE), "正确")
            .replace(Regex("\\bwrong\\b", RegexOption.IGNORE_CASE), "错误")
            .replace(Regex("\\btrue\\b", RegexOption.IGNORE_CASE), "真")
            .replace(Regex("\\bfalse\\b", RegexOption.IGNORE_CASE), "假")
            .replace(Regex("\\breal\\b", RegexOption.IGNORE_CASE), "真实")
            .replace(Regex("\\bvirtual\\b", RegexOption.IGNORE_CASE), "虚拟")
            .replace(Regex("\\bdigital\\b", RegexOption.IGNORE_CASE), "数字")
            .replace(Regex("\\bonline\\b", RegexOption.IGNORE_CASE), "在线")
            .replace(Regex("\\boffline\\b", RegexOption.IGNORE_CASE), "离线")
            .replace(Regex("\\bcloud\\b", RegexOption.IGNORE_CASE), "云端")
            .replace(Regex("\\bedge\\b", RegexOption.IGNORE_CASE), "边缘")
            .replace(Regex("\\bdevice\\b", RegexOption.IGNORE_CASE), "设备")
            .replace(Regex("\\bserver\\b", RegexOption.IGNORE_CASE), "服务器")
            .replace(Regex("\\bdata\\b", RegexOption.IGNORE_CASE), "数据")
            .replace(Regex("\\bprivacy\\b", RegexOption.IGNORE_CASE), "隐私")
            .replace(Regex("\\bsecurity\\b", RegexOption.IGNORE_CASE), "安全")
            .replace(Regex("\\btrust\\b", RegexOption.IGNORE_CASE), "信任")
            .replace(Regex("\\bsafe\\b", RegexOption.IGNORE_CASE), "安全")
            .replace(Regex("\\brisk\\b", RegexOption.IGNORE_CASE), "风险")
            .replace(Regex("\\bdanger\\b", RegexOption.IGNORE_CASE), "危险")
            .replace(Regex("\\bproblem\\b", RegexOption.IGNORE_CASE), "问题")
            .replace(Regex("\\bsolution\\b", RegexOption.IGNORE_CASE), "解决方案")
            .replace(Regex("\\banswer\\b", RegexOption.IGNORE_CASE), "答案")
            .replace(Regex("\\bresult\\b", RegexOption.IGNORE_CASE), "结果")
            .replace(Regex("\\bgoal\\b", RegexOption.IGNORE_CASE), "目标")
            .replace(Regex("\\bmission\\b", RegexOption.IGNORE_CASE), "使命")
            .replace(Regex("\\bvision\\b", RegexOption.IGNORE_CASE), "愿景")
            .replace(Regex("\\bdream\\b", RegexOption.IGNORE_CASE), "梦想")
            .replace(Regex("\\bhope\\b", RegexOption.IGNORE_CASE), "希望")
            .replace(Regex("\\bfear\\b", RegexOption.IGNORE_CASE), "恐惧")
            .replace(Regex("\\banger\\b", RegexOption.IGNORE_CASE), "愤怒")
            .replace(Regex("\\bjoy\\b", RegexOption.IGNORE_CASE), "快乐")
            .replace(Regex("\\bsadness\\b", RegexOption.IGNORE_CASE), "悲伤")
            .replace(Regex("\\blove\\b", RegexOption.IGNORE_CASE), "爱")
            .replace(Regex("\\bhate\\b", RegexOption.IGNORE_CASE), "恨")
            .replace(Regex("\\bpeace\\b", RegexOption.IGNORE_CASE), "和平")
            .replace(Regex("\\bwar\\b", RegexOption.IGNORE_CASE), "战争")
            .replace(Regex("\\bfreedom\\b", RegexOption.IGNORE_CASE), "自由")
            .replace(Regex("\\bjustice\\b", RegexOption.IGNORE_CASE), "正义")
            .replace(Regex("\\bequality\\b", RegexOption.IGNORE_CASE), "平等")
            .replace(Regex("\\bhumanity\\b", RegexOption.IGNORE_CASE), "人性")
            .replace(Regex("\\bmankind\\b", RegexOption.IGNORE_CASE), "人类")
            .replace(Regex("\\bhumankind\\b", RegexOption.IGNORE_CASE), "人类")
            .replace(Regex("\\bpeople\\b", RegexOption.IGNORE_CASE), "人们")
            .replace(Regex("\\bsociety\\b", RegexOption.IGNORE_CASE), "社会")
            .replace(Regex("\\bworld\\b", RegexOption.IGNORE_CASE), "世界")
            .replace(Regex("\\bglobal\\b", RegexOption.IGNORE_CASE), "全球")
            .replace(Regex("\\blocal\\b", RegexOption.IGNORE_CASE), "本地")
            .replace(Regex("\\bnational\\b", RegexOption.IGNORE_CASE), "国家")
            .replace(Regex("\\binternational\\b", RegexOption.IGNORE_CASE), "国际")
            .replace(Regex("\\bdomestic\\b", RegexOption.IGNORE_CASE), "国内")
            .replace(Regex("\\bforeign\\b", RegexOption.IGNORE_CASE), "国外")
            .replace(Regex("\\beast\\b", RegexOption.IGNORE_CASE), "东")
            .replace(Regex("\\bwest\\b", RegexOption.IGNORE_CASE), "西")
            .replace(Regex("\\bnorth\\b", RegexOption.IGNORE_CASE), "北")
            .replace(Regex("\\bsouth\\b", RegexOption.IGNORE_CASE), "南")
            .replace(Regex("\\bup\\b", RegexOption.IGNORE_CASE), "上")
            .replace(Regex("\\bdown\\b", RegexOption.IGNORE_CASE), "下")
            .replace(Regex("\\bleft\\b", RegexOption.IGNORE_CASE), "左")
            .replace(Regex("\\bright\\b", RegexOption.IGNORE_CASE), "右")
            .replace(Regex("\\bin\\b", RegexOption.IGNORE_CASE), "内")
            .replace(Regex("\\bout\\b", RegexOption.IGNORE_CASE), "外")
            .replace(Regex("\\bover\\b", RegexOption.IGNORE_CASE), "上方")
            .replace(Regex("\\bunder\\b", RegexOption.IGNORE_CASE), "下方")
            .replace(Regex("\\babove\\b", RegexOption.IGNORE_CASE), "上方")
            .replace(Regex("\\bbelow\\b", RegexOption.IGNORE_CASE), "下方")
            .replace(Regex("\\bbetween\\b", RegexOption.IGNORE_CASE), "之间")
            .replace(Regex("\\bamong\\b", RegexOption.IGNORE_CASE), "之中")
            .replace(Regex("\\bthrough\\b", RegexOption.IGNORE_CASE), "通过")
            .replace(Regex("\\bacross\\b", RegexOption.IGNORE_CASE), "跨")
            .replace(Regex("\\baround\\b", RegexOption.IGNORE_CASE), "周围")
            .replace(Regex("\\bagainst\\b", RegexOption.IGNORE_CASE), "反对")
            .replace(Regex("\\btoward\\b", RegexOption.IGNORE_CASE), "向")
            .replace(Regex("\\baway\\b", RegexOption.IGNORE_CASE), "远离")
            .replace(Regex("\\bclose\\b", RegexOption.IGNORE_CASE), "关闭")
            .replace(Regex("\\bopen\\b", RegexOption.IGNORE_CASE), "打开")
            .replace(Regex("\\benter\\b", RegexOption.IGNORE_CASE), "进入")
            .replace(Regex("\\bexit\\b", RegexOption.IGNORE_CASE), "退出")
            .replace(Regex("\\bstart\\b", RegexOption.IGNORE_CASE), "开始")
            .replace(Regex("\\bstop\\b", RegexOption.IGNORE_CASE), "停止")
            .replace(Regex("\\bend\\b", RegexOption.IGNORE_CASE), "结束")
            .replace(Regex("\\bbegin\\b", RegexOption.IGNORE_CASE), "开始")
            .replace(Regex("\\bfinish\\b", RegexOption.IGNORE_CASE), "完成")
            .replace(Regex("\\bcomplete\\b", RegexOption.IGNORE_CASE), "完成")
            .replace(Regex("\\bsuccess\\b", RegexOption.IGNORE_CASE), "成功")
            .replace(Regex("\\bfailure\\b", RegexOption.IGNORE_CASE), "失败")
            .replace(Regex("\\bloss\\b", RegexOption.IGNORE_CASE), "损失")
            .replace(Regex("\\bgain\\b", RegexOption.IGNORE_CASE), "收益")
            .replace(Regex("\\bprofit\\b", RegexOption.IGNORE_CASE), "利润")
            .replace(Regex("\\brevenue\\b", RegexOption.IGNORE_CASE), "收入")
            .replace(Regex("\\bcost\\b", RegexOption.IGNORE_CASE), "成本")
            .replace(Regex("\\bprice\\b", RegexOption.IGNORE_CASE), "价格")
            .replace(Regex("\\bmoney\\b", RegexOption.IGNORE_CASE), "钱")
            .replace(Regex("\\bwealth\\b", RegexOption.IGNORE_CASE), "财富")
            .replace(Regex("\\brich\\b", RegexOption.IGNORE_CASE), "富有")
            .replace(Regex("\\bpoor\\b", RegexOption.IGNORE_CASE), "贫穷")
            .replace(Regex("\\bhealth\\b", RegexOption.IGNORE_CASE), "健康")
            .replace(Regex("\\blife\\b", RegexOption.IGNORE_CASE), "生命")
            .replace(Regex("\\bdeath\\b", RegexOption.IGNORE_CASE), "死亡")
            .replace(Regex("\\bbirth\\b", RegexOption.IGNORE_CASE), "出生")
            .replace(Regex("\\btime\\b", RegexOption.IGNORE_CASE), "时间")
            .replace(Regex("\\bspace\\b", RegexOption.IGNORE_CASE), "空间")
            .replace(Regex("\\benergy\\b", RegexOption.IGNORE_CASE), "能量")
            .replace(Regex("\\bmatter\\b", RegexOption.IGNORE_CASE), "物质")
            .replace(Regex("\\bforce\\b", RegexOption.IGNORE_CASE), "力量")
            .replace(Regex("\\bpower\\b", RegexOption.IGNORE_CASE), "力量")
            .replace(Regex("\\bstrength\\b", RegexOption.IGNORE_CASE), "强度")
            .replace(Regex("\\bweakness\\b", RegexOption.IGNORE_CASE), "弱点")
            .replace(Regex("\\bstrength\\b", RegexOption.IGNORE_CASE), "优势")
            .replace(Regex("\\bweakness\\b", RegexOption.IGNORE_CASE), "劣势")
            .replace(Regex("\\badvantage\\b", RegexOption.IGNORE_CASE), "优势")
            .replace(Regex("\\bdisadvantage\\b", RegexOption.IGNORE_CASE), "劣势")
            .replace(Regex("\\bbenefit\\b", RegexOption.IGNORE_CASE), "收益")
            .replace(Regex("\\bdrawback\\b", RegexOption.IGNORE_CASE), "缺点")
            .replace(Regex("\\bmerit\\b", RegexOption.IGNORE_CASE), "优点")
            .replace(Regex("\\bdemerit\\b", RegexOption.IGNORE_CASE), "缺点")
            .replace(Regex("\\bpro\\b", RegexOption.IGNORE_CASE), "优点")
            .replace(Regex("\\bcon\\b", RegexOption.IGNORE_CASE), "缺点")

        // 清理多余空格和标点
        result = result.replace(Regex("\\s+"), " ").trim()
        result = result.replace(Regex("\\s*([，。！？])"), "$1")
        result = result.replace(Regex("([，。！？])\\s*"), "$1")

        return result
    }
}
