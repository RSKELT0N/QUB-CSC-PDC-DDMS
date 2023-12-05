Vagrant.configure("2") do |config|
  # Define VM configurations
  (1..3).each do |i| # Adjust the number of VMs as needed
    config.vm.define "peer-#{i}" do |node|
      config.vm.box = "reze/Maven-Java"
      config.vm.box_version = "1.0.0" 
      
      host_port = 50000 + i
      node.vm.network "forwarded_port", guest: 52223, host: host_port

      node.vm.synced_folder ".", "/vagrant"

      node.vm.provision "shell", inline: <<-SHELL
        # Update repositories and install Java
             
        # sudo pacman -Syyu extra/openjdk17-doc --noconfirm

        # Run your Java program
        cd /vagrant/
        ls -al
        javac *.java
        nohup java Main 8080 &
      SHELL
    end
  end
end
