FROM flurdy/oracle-java8
MAINTAINER Shohei Miyashita <kei@kamasu.jp>

# copy project filesroot
ADD ./ /root

# run play
EXPOSE 40003
WORKDIR /root

CMD ./target/universal/stage/bin/twitter-finance -Dhttp.port=80 -J-Xms128M -J-Xmx512m -J-server

