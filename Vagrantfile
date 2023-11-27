Vagrant.configure("2") do |config|
  # Define VM configurations
  (1..3).each do |i| # Adjust the number of VMs as needed
    config.vm.define "peer-#{i}" do |node|
      config.vm.box = "ubuntu/bionic64" 
      
      host_port = 50000 + i
      node.vm.network "forwarded_port", guest: 52223, host: host_port

      node.vm.synced_folder ".", "/vagrant"

      node.vm.provision "shell", inline: <<-SHELL
        # Update repositories and install Java
        sudo apt-get update -y && sudo apt-get upgrade -y

        wget https://download.oracle.com/java/19/archive/jdk-19.0.1_linux-x64_bin.deb
        sudo apt-get -qqy install ./jdk-19.0.1_linux-x64_bin.deb
        sudo update-alternatives --install /usr/bin/java java /usr/lib/jvm/jdk-19/bin/java 1919
        
        # Run your Java program
        cd /vagrant/src/
        javac *.java
        java Main 8080
      SHELL
    end
  end
end
