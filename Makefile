all::


all:: src/multiplexer/Multiplexer.java
src/multiplexer/Multiplexer.java: Multiplexer.proto
	protoc --java_out=src $<
