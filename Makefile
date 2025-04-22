V_FILE_GEN   = build/ysyxSoCFPGA.sv
SCALA_FILES = $(shell find src/ -name "*.scala")

$(V_FILE_GEN): $(SCALA_FILES)
	mill -i ysyxsoc.runMain ysyx.Elaborate --target-dir $(@D)
	sed -i -e 's/_\(aw\|ar\|w\|r\|b\)_\(\|bits_\)/_\1/g' $@
	sed -i '/firrtl_black_box_resource_files.f/, $$d' $@

verilog: $(V_FILE_GEN)

clean:
	-rm -rf build/

dev-init:
	git submodule update --init --recursive
	cd rocket-chip && git apply ../patch/rocket-chip.patch

.PHONY: verilog clean dev-init
