#! /bin/bash

echo "Donwloading Go..."
wget -O go.tgz https://dl.google.com/go/go1.12.7.linux-amd64.tar.gz
echo "Donwload Complete!"

echo "Unpacking Files..."
sudo tar -C /usr/local -xzf go.tgz
rm -f go.tgz
echo "Complete!"

echo 'Adding /usr/local/go/bin to $PATH'
echo 'PATH=$PATH:/usr/local/go/bin' >> ~/.cs162.bashrc
export PATH=$PATH:/usr/local/go/bin
echo "Complete!"
