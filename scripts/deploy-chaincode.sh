#!/usr/bin/env bash
# =========================================================================
# AGRISHIELD CHAINCODE DEPLOYMENT SCRIPT (MrlAmuContract)
# Packages, installs, approves, and commits Java Chaincode on Fabric 2.5
# =========================================================================
set -e

CC_NAME="mrl-amu-contract"
CC_VERSION="1.0"
CC_SEQUENCE="1"
CHANNEL_NAME="farmchannel"
CC_SRC_PATH="/opt/gopath/src/github.com/chaincode"

echo "=== [1/5] Building Java Chaincode JAR ==="
cd /chaincode && mvn clean package -DskipTests
cd -

echo "=== [2/5] Packaging Chaincode ==="
docker exec peer0.org1.example.com peer lifecycle chaincode package ${CC_NAME}.tar.gz \
  --path ${CC_SRC_PATH} \
  --lang java \
  --label ${CC_NAME}_${CC_VERSION}

echo "=== [3/5] Installing Chaincode on Org1 and Org2 Peers ==="
docker exec peer0.org1.example.com peer lifecycle chaincode install ${CC_NAME}.tar.gz
docker exec peer0.org2.example.com peer lifecycle chaincode install ${CC_NAME}.tar.gz

PACKAGE_ID=$(docker exec peer0.org1.example.com peer lifecycle chaincode calculatepackageid ${CC_NAME}.tar.gz)
echo "Installed Chaincode Package ID: ${PACKAGE_ID}"

echo "=== [4/5] Approving Chaincode Definition for Org1 & Org2 ==="
docker exec peer0.org1.example.com peer lifecycle chaincode approveformyorg \
  -o orderer.example.com:7050 \
  --channelID ${CHANNEL_NAME} \
  --name ${CC_NAME} \
  --version ${CC_VERSION} \
  --package-id ${PACKAGE_ID} \
  --sequence ${CC_SEQUENCE}

docker exec peer0.org2.example.com peer lifecycle chaincode approveformyorg \
  -o orderer.example.com:7050 \
  --channelID ${CHANNEL_NAME} \
  --name ${CC_NAME} \
  --version ${CC_VERSION} \
  --package-id ${PACKAGE_ID} \
  --sequence ${CC_SEQUENCE}

echo "=== [5/5] Committing Chaincode Definition to ${CHANNEL_NAME} ==="
docker exec peer0.org1.example.com peer lifecycle chaincode commit \
  -o orderer.example.com:7050 \
  --channelID ${CHANNEL_NAME} \
  --name ${CC_NAME} \
  --version ${CC_VERSION} \
  --sequence ${CC_SEQUENCE} \
  --peerAddresses peer0.org1.example.com:7051 \
  --peerAddresses peer0.org2.example.com:9051

echo "=== Chaincode ${CC_NAME} successfully committed to ${CHANNEL_NAME} ==="
