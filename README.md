首先你需要进行一些配置和初始化工作:

根据mill的文档介绍安装mill
可通过`mill --version`检查安装是否成功; 此外, rocket-chip项目要求mill的版本不低于0.11, 如果你发现mill的版本不符合要求, 请安装最新版本的mill
在ysyxSoC/目录下运行`make dev-init`命令, 拉取rocket-chip项目
以上两个步骤只需要进行一次即可. 完成上述配置后, 在ysyxSoC/目录下运行`make verilog`, 生成的Verilog文件位于ysyxSoC/build/ysyxSoCFull.v.
