.PHONY: all build clean parteA parteB benchmark

all: build

build:
	rm -rf out && mkdir -p out && \
	find src -name '*.java' -print0 | xargs -0 javac -encoding UTF-8 -d out

parteA: build
	java -cp out tp3.partieA.LamportExclusion

parteB: build
	java -cp out tp3.partieB.LamportDistributedMutex

benchmark: build
	java -cp out tp3.partieB.Benchmark

clean:
	rm -rf out
