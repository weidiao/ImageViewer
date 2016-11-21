##设计
只通过鼠标就可以查看图片,用三种模式
* 缩放模式,滚轮控制缩放,通过鼠标左键单击进入此模式
* 平移查看模式,滚轮控制图片左右或者上下移动,鼠标右键单击进入此模式
* 浏览图片模式,鼠标左键双击进入此模式

##运行此程序
打开控制台,运行build和run即可.
可以将照片的默认打开方式设置为run.cmd,这样就可以始终用ImageViwer查看.

## 实现
使用BufferedImage只能保留一帧图像,无法描述gif动图.
使用如下方法可以实现动图:
* paint不停重绘
* Image img= getToolkit().getImage(path);
	这个img的实际类型是class sun.awt.image.ToolkitImage,可通过img.getClass()查看

关于rec这个变量:
一切算法围绕rec这个变量展开,rec的意义就是视窗.
整个照片查看器,原图片img变量是不发生改变的,发生改变的只有rec这个矩形变量.
平移图片改变平移rec.x和rec.y
缩放图片相当于改变视窗rec.w和rec.h
* rec.x,rec.y表示当前屏幕中心像素对应视窗中的哪个点
* rec.w,rec.h表示当前视窗的宽度和高度

一开始还想加配置选项,通过Property保存配置,用来设置背景色和窗口状态.但一想这有点过度设计,所以只实现基本功能.