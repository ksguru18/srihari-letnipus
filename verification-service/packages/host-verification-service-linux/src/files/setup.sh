#!/bin/bash
# WARNING:
# *** do NOT use TABS for indentation, use SPACES
# *** TABS will cause errors in some linux distributions

# Host Verification Service install script
# Outline:
# 1. export default settings
# 2. source the "functions.sh"
# 3. determine if we are installing as root or non-root
# 4. Define directory layout
# 5. Install package dependencies
# 5.1 Install Java
# 5.2 Install prerequisites
# 5.3 Install logrotate
# 5.4 Install postgres
# 6. Copy scripts to application
# 7. extract mtwilson
# 8. configure mtwilson TLS policies
# 9. Update configurations
# 10. ASCTL SETUP
# 11. setup the director, unless the NOSETUP variable is defined
# 12. tag service installation
# 13. config logrotate
# 14. Register mtwilson as a startup script
# 15. Start mtwilson service

#####

# load installer environment file, if present
if [ -f ~/mtwilson.env ]; then
  echo "Loading environment variables from $(cd ~ && pwd)/mtwilson.env"
  . ~/mtwilson.env
  env_file_exports=$(cat ~/mtwilson.env | grep -E '^[A-Z0-9_]+\s*=' | cut -d = -f 1)
  if [ -n "$env_file_exports" ]; then eval export $env_file_exports; fi
else
  echo "No environment file"
fi
##########################################################################################

#########################################################################################
# 2. export default settings
# can override by exporting them before running
# the installer or by including them in mtwilson.env
export INSTALL_LOG_FILE=${INSTALL_LOG_FILE:-/tmp/mtwilson-install.log}
export INSTALLED_MARKER_FILE=${INSTALLED_MARKER_FILE:-/var/opt/intel/.mtwilsonInstalled}
export LOG_ROTATION_PERIOD=${LOG_ROTATION_PERIOD:-monthly}
export LOG_COMPRESS=${LOG_COMPRESS:-compress}
export LOG_DELAYCOMPRESS=${LOG_DELAYCOMPRESS:-delaycompress}
export LOG_COPYTRUNCATE=${LOG_COPYTRUNCATE:-copytruncate}
export LOG_SIZE=${LOG_SIZE:-1G}
export LOG_OLD=${LOG_OLD:-12}
export DATABASE_HOSTNAME=${DATABASE_HOSTNAME:-127.0.0.1}
export DATABASE_PORTNUM=${DATABASE_PORTNUM:-5432}
export DATABASE_SCHEMA=${DATABASE_SCHEMA:-mw_as}
export DATABASE_VENDOR=postgres
export POSTGRES_HOSTNAME=${DATABASE_HOSTNAME}
export POSTGRES_PORTNUM=${DATABASE_PORTNUM}
export POSTGRES_DATABASE=${DATABASE_SCHEMA}
export POSTGRESQL_KEEP_PGPASS=${POSTGRESQL_KEEP_PGPASS:-true}
export INSTALL_PKGS=${INSTALL_PKGS:-"java logrotate postgres privacyca SERVICES PORTALS"}
export MTWILSON_TLS_POLICY_ALLOW=${MTWILSON_TLS_POLICY_ALLOW:-"certificate,certificate-digest,public-key,public-key-digest,TRUST_FIRST_CERTIFICATE,INSECURE"}
export MTWILSON_DEFAULT_TLS_POLICY_ID=${MTWILSON_DEFAULT_TLS_POLICY_ID:-"TRUST_FIRST_CERTIFICATE"}
export JAVA_REQUIRED_VERSION=${JAVA_REQUIRED_VERSION:-1.8}
export PRIVACYCA_DOWNLOAD_USERNAME=${PRIVACYCA_DOWNLOAD_USERNAME:-pca-admin}
export PRIVACYCA_DOWNLOAD_PASSWORD=${PRIVACYCA_DOWNLOAD_PASSWORD:-PrivacyCaPassword}
export POSTGRES_REQUIRED_VERSION=${POSTGRES_REQUIRED_VERSION:-9.4}
export DATABASE_VENDOR=${DATABASE_VENDOR:-postgres}
export ADD_POSTGRESQL_REPO=${ADD_POSTGRESQL_REPO:-yes}
export MTWILSON_API_SSL_VERIFY_HOSTNAME=${MTWILSON_API_SSL_VERIFY_HOSTNAME:-false}
export PRIVACYCA_KEYGEN_URANDOM=${PRIVACYCA_KEYGEN_URANDOM:-yes}
export MTWILSON_TAG_ADMIN_USERNAME=${MTWILSON_TAG_ADMIN_USERNAME:-tagentadmin}
export MTWILSON_TAG_ADMIN_PASSWORD=${MTWILSON_TAG_ADMIN_PASSWORD:-TagProvisionPassword}
export TAG_PROVISION_NOCACHE=${TAG_PROVISION_NOCACHE:-true}

export MTWILSON_HOME=${MTWILSON_HOME:-/opt/mtwilson}

# the layout setting is used only by this script
# and it is not saved or used by the app script
MTWILSON_LAYOUT=${MTWILSON_LAYOUT:-home}

# the env directory is not configurable; it is defined as MTWILSON_HOME/env.d
# and the administrator may use a symlink if necessary to place it anywhere else
export MTWILSON_ENV=$MTWILSON_HOME/env

# load application environment variables if already defined
if [ -d $MTWILSON_ENV ]; then
  MTWILSON_ENV_FILES=$(ls -1 $MTWILSON_ENV/*)
  for env_file in $MTWILSON_ENV_FILES; do
    . $env_file
    env_file_exports=$(cat $env_file | grep -E '^[A-Z0-9_]+\s*=' | cut -d = -f 1)
    if [ -n "$env_file_exports" ]; then eval export $env_file_exports; fi
  done
fi

# ensure we can write to the log file
touch $INSTALL_LOG_FILE >/dev/null 2>&1
if [ -f $INSTALL_LOG_FILE ] && [ ! -w $INSTALL_LOG_FILE ]; then
  echo "Cannot write to install log file: $INSTALL_LOG_FILE"
  exit 1
fi

# 2. source the "functions.sh"
if [ -f functions ]; then . functions; else echo "Missing file: functions"; exit 1; fi

# 3. determine if we are installing as root or non-root
if [ "$(whoami)" == "root" ]; then
  # create a mtwilson user if there isn't already one created
  export MTWILSON_USERNAME=${MTWILSON_USERNAME:-mtwilson}
  if ! getent passwd $MTWILSON_USERNAME >/dev/null 2>&1; then
    useradd --comment "Mt Wilson" --home $MTWILSON_HOME --system --shell /bin/false $MTWILSON_USERNAME
    usermod --lock $MTWILSON_USERNAME
    # note: to assign a shell and allow login you can run "usermod --shell /bin/bash --unlock $MTWILSON_USERNAME"
  fi
else
  # already running as mtwilson user
  export MTWILSON_USERNAME=$(whoami)
  echo_warning "Running as $MTWILSON_USERNAME; if installation fails try again as root"
  if [ ! -w "$MTWILSON_HOME" ] && [ ! -w $(dirname $MTWILSON_HOME) ]; then
    export MTWILSON_HOME=$(cd ~ && pwd)
  fi
  echo_warning "Installing as $MTWILSON_USERNAME into $MTWILSON_HOME"  
fi

# 4. Define directory layout
chown $MTWILSON_USERNAME:$MTWILSON_USERNAME $INSTALL_LOG_FILE
date > $INSTALL_LOG_FILE

# computed values
export PATH=$MTWILSON_HOME/bin:$PATH

# define application directory layout
if [ "$MTWILSON_LAYOUT" == "linux" ]; then
  export MTWILSON_CONFIGURATION=${MTWILSON_CONFIGURATION:-/etc/mtwilson}
  export MTWILSON_REPOSITORY=${MTWILSON_REPOSITORY:-/var/opt/mtwilson}
  export MTWILSON_LOGS=${MTWILSON_LOGS:-/var/log/mtwilson}
elif [ "$MTWILSON_LAYOUT" == "home" ]; then
  export MTWILSON_CONFIGURATION=${MTWILSON_CONFIGURATION:-$MTWILSON_HOME/configuration}
  export MTWILSON_REPOSITORY=${MTWILSON_REPOSITORY:-$MTWILSON_HOME/repository}
  export MTWILSON_LOGS=${MTWILSON_LOGS:-$MTWILSON_HOME/logs}
fi
export MTWILSON_BIN=${MTWILSON_BIN:-$MTWILSON_HOME/bin}
export MTWILSON_JAVA=${MTWILSON_JAVA:-$MTWILSON_HOME/java}
export MTWILSON_BACKUP=${MTWILSON_BACKUP:-$MTWILSON_REPOSITORY/backup}


#If user is non root make sure all prereq directories are created and owned by nonroot user
if [ "$(whoami)" != "root" ]; then
  if [ ! -d $MTWILSON_HOME ]; then
   echo_failure "$MTWILSON_HOME is not available. Please create one and change its owner to $MTWILSON_USERNAME."
   exit 1
  elif [ "${home=`stat -c '%U' $MTWILSON_HOME`}" != "$MTWILSON_USERNAME" ]; then
   echo_failure "$MTWILSON_HOME is not owned by $MTWILSON_USERNAME. Please update the owner."
   exit 1
  elif [ ! -d /opt/intel ]; then
   echo_failure "/opt/intel is not available. Please create one and change its owner to $MTWILSON_USERNAME."
   exit 1
  elif [ "${optintel=`stat -c '%U' /opt/intel`}" != "$MTWILSON_USERNAME" ]; then
   echo_failure "/opt/intel is not owned by $MTWILSON_USERNAME. Please update the owner."
   exit 1
  elif [ ! -d /etc/intel ]; then
   echo_failure "/etc/intel is not available. Please create one and change its owner to $MTWILSON_USERNAME."
   exit 1
  elif [ "${etcintel=`stat -c '%U' /etc/intel`}" != "$MTWILSON_USERNAME" ]; then
   echo_failure "/etc/intel is not owned by $MTWILSON_USERNAME. Please update the owner."
   exit 1
  elif [ ! -d /opt/mtwilson ]; then
   echo_failure "/opt/mtwilson is not available. Please create one and change its owner to $MTWILSON_USERNAME."
   exit 1
  elif [ "${optmtw=`stat -c '%U' /opt/mtwilson`}" != "$MTWILSON_USERNAME" ]; then
   echo_failure "/opt/mtwilson is not owned by $MTWILSON_USERNAME. Please update the owner."
   exit 1
  elif [ ! -d /var/opt/intel ]; then
   echo_failure "/var/opt/intel is not available. Please create one and change its owner to $MTWILSON_USERNAME."
   exit 1
  elif [ "${varoptintel=`stat -c '%U' /var/opt/intel`}" != "$MTWILSON_USERNAME" ]; then
   echo_failure "/var/opt/intel is not owned by $MTWILSON_USERNAME. Please update the owner."
   exit 1
  elif [ ! -d /usr/local/share/mtwilson ]; then
   echo_failure "/usr/local/share/mtwilson is not available. Please create one and change its owner to $MTWILSON_USERNAME."
   exit 1
  elif [ "${ulsmtw=`stat -c '%U' /usr/local/share/mtwilson`}" != "$MTWILSON_USERNAME" ]; then
   echo_failure "/usr/local/share/mtwilson is not owned by $MTWILSON_USERNAME. Please update the owner."
   exit 1
  elif ! [[ -d /etc/intel/cloudsecurity || -L /etc/intel/cloudsecurity ]]; then
   echo_failure "/etc/intel/cloudsecurity is not available. Please create one and change its owner to $MTWILSON_USERNAME."
   exit 1
  elif [ "${eics=`stat -c '%U' /etc/intel/cloudsecurity`}" != "$MTWILSON_USERNAME" ]; then
   echo_failure "/etc/intel/cloudsecurity is not owned by $MTWILSON_USERNAME. Please update the owner."
   exit 1
  elif [ ! -d $MTWILSON_HOME ]; then
   echo_failure "$MTWILSON_HOME is not available. Please create one and change its owner to $MTWILSON_USERNAME."
   exit 1
  elif [ "${mtwhome=`stat -c '%U' $MTWILSON_HOME`}" != "$MTWILSON_USERNAME" ]; then
   echo_failure "$MTWILSON_HOME is not owned by $MTWILSON_USERNAME. Please update the owner."
   exit 1
  else 
   echo "Prerequisite check is successful"
  fi
fi



# if an existing mtwilson is already running, stop it while we install
echo "Checking for previously-installed Mt Wilson..." >>$INSTALL_LOG_FILE
prev_mtwilson="$(which mtwilson 2>/dev/null)"
if [ -n "$prev_mtwilson" ] && [ "$(whoami)" == "root" ]; then
  # stop mtwilson; this sometimes does not work
  $prev_mtwilson stop
  echo "After '$prev_mtwilson stop', checking status again..." >>$INSTALL_LOG_FILE
  $prev_mtwilson status >>$INSTALL_LOG_FILE
  # remove previous mtwilson script
  rm -f $prev_mtwilson
fi

# if upgrading as non-root user, admin must grant read/write permission to /etc/intel/cloudsecurity before running installer
if [ -L $MTWILSON_CONFIGURATION ]; then rm -f $MTWILSON_CONFIGURATION; fi
if [ -L /etc/intel/cloudsecurity ]; then rm -f /etc/intel/cloudsecurity; fi
if [ -d /etc/intel/cloudsecurity ]; then
  echo "Prior configuration exists:" >>$INSTALL_LOG_FILE
  ls -l /etc/intel >>$INSTALL_LOG_FILE
  if [ -w /etc/intel/cloudsecurity ]; then
    echo "Migrating configuration from /etc/intel/cloudsecurity to $MTWILSON_CONFIGURATION" >>$INSTALL_LOG_FILE
    mkdir -p $MTWILSON_CONFIGURATION
    cp -r /etc/intel/cloudsecurity/* $MTWILSON_CONFIGURATION
    chown -R $MTWILSON_USERNAME:$MTWILSON_USERNAME $MTWILSON_CONFIGURATION
    rm -rf /etc/intel/cloudsecurity
    ln -s $MTWILSON_CONFIGURATION /etc/intel/cloudsecurity
  else
    echo_failure "Cannot migrate configuration from /etc/intel/cloudsecurity to $MTWILSON_CONFIGURATION"
    exit 1
  fi
else  
  mkdir -p $MTWILSON_CONFIGURATION
  if [ $? -ne 0 ]; then
    echo_failure "Cannot create directory: $MTWILSON_CONFIGURATION"
    exit 1
  fi
  mkdir -p /etc/intel
  ln -s $MTWILSON_CONFIGURATION /etc/intel/cloudsecurity
  if [ $? -ne 0 ]; then
    echo_failure "Cannot link configuration from /etc/intel/cloudsecurity to $MTWILSON_CONFIGURATION"
    exit 1
  fi
fi

export MTWILSON_SERVICE_PROPERTY_FILES=/etc/intel/cloudsecurity
export MTWILSON_OPT_INTEL=/opt/intel
export MTWILSON_ETC_INTEL=/etc/intel
# If configuration is already in /etc/intel/cloudsecurity (upgrade or reinstall)
# then symlink /opt/mtwilson/configuration -> /etc/intel/cloudsecurity 

# If configuration is in /opt/mtwilson/configuration and there is no symlink
# in /etc/intel/cloudsecurity then we create one now
if [ -d "$MTWILSON_CONFIGURATION" ] && [ ! -L "$MTWILSON_CONFIGURATION" ] && [ "/etc/intel/clousecurity" != "$MTWILSON_CONFIGURATION" ] && [ ! -d /etc/intel/cloudsecurity ] && [ ! -L /etc/intel/cloudsecurity ]; then
  ln -s $MTWILSON_CONFIGURATION /etc/intel/cloudsecurity
fi

# Check for incorrect link and remove it 
if [ -L "$MTWILSON_CONFIGURATION/cloudsecurity" ]; then
  rm "$MTWILSON_CONFIGURATION/cloudsecurity"
fi

set_owner_for_mtwilson_directories() {
  for directory in $MTWILSON_HOME $MTWILSON_CONFIGURATION $MTWILSON_JAVA $MTWILSON_BIN $MTWILSON_ENV $MTWILSON_REPOSITORY $MTWILSON_LOGS $MTWILSON_SERVICE_PROPERTY_FILES $MTWILSON_OPT_INTEL $MTWILSON_ETC_INTEL; do
    chown -R $MTWILSON_USERNAME:$MTWILSON_USERNAME $directory
  done
}

# ensure application directories exist (chown will be repeated near end of this script, after setup)
for directory in $MTWILSON_HOME $MTWILSON_CONFIGURATION $MTWILSON_ENV $MTWILSON_REPOSITORY $MTWILSON_LOGS $MTWILSON_BIN $MTWILSON_JAVA $MTWILSON_SERVICE_PROPERTY_FILES $MTWILSON_OPT_INTEL $MTWILSON_ETC_INTEL; do
  # mkdir -p will return 0 if directory exists or is a symlink to an existing directory or directory and parents can be created
  mkdir -p $directory
  if [ $? -ne 0 ]; then
    echo_failure "Cannot create directory: $directory"
    exit 1
  fi
  chmod 700 $directory
done
touch ${MTWILSON_LOGS}/mtwilson.log
touch ${MTWILSON_LOGS}/mtwilson-log4j.log
set_owner_for_mtwilson_directories

########################################################################################################################
# 5. Install package dependencies
# 5.1 Install Java
# mtwilson requires java 1.8 or later
echo "Installing Java..."
JAVA_REQUIRED_VERSION=${JAVA_REQUIRED_VERSION:-1.8}
# in 3.0, java home is now under trustagent home by default
if [ "$IS_RPM" != "true" ]; then
  java_install_openjdk
  if [ $? -ne 0 ]; then echo_failure "Failed to install openjdk through package manager"; exit 1; fi
fi

# 5.2 Install prerequisites
if [ "$(whoami)" == "root" ]; then
  if [ "$IS_RPM" != "true" ]; then
    MTWILSON_YUM_PACKAGES="zip unzip openssl xmlstarlet wget net-tools policycoreutils-python"
  fi
  MTWILSON_APT_PACKAGES="zip unzip openssl xmlstarlet policycoreutils "
  MTWILSON_YAST_PACKAGES="zip unzip openssl xmlstarlet"
  MTWILSON_ZYPPER_PACKAGES="zip unzip openssl xmlstarlet"
  if [ "$IS_RPM" != "true" ]; then
    install_packages "Installer requirements" "MTWILSON"
    if [ $? -ne 0 ]; then echo_failure "Failed to install prerequisites through package manager"; exit 1; fi
  fi
else
  echo_warning "You must be root to install prerequisites through package manager"
fi

if [ -z "$INSTALL_PKGS" ]; then
  INSTALL_PKGS="postgres privacyca SERVICES"
fi

FIRST=0
#loop through INSTALL_PKG and set each entry to true
for i in $INSTALL_PKGS; do
  pkg=`echo $i | tr '[A-Z]' '[a-z]'`
  eval opt_$pkg="true"
  if [ $FIRST == 0 ]; then
    FIRST=1;
    LIST=$pkg
  else
    LIST=$LIST", "$pkg
  fi
done

find_installer() {
  local installer="${1}"
  binfile=`ls -1 $installer-*.bin 2>/dev/null | head -n 1`
  echo $binfile
}


logrotate_installer=`find_installer log-rotate`


# 5.3 Install logrotate
if [ ! -z "$opt_logrotate" ]; then
  echo "Installing Log Rotate..." | tee -a  $INSTALL_LOG_FILE
  ./$logrotate_installer
  if [ $? -ne 0 ]; then echo_failure "Failed to install log rotation"; exit -1; fi
  #echo "Log Rotate installed" | tee -a  $INSTALL_LOG_FILE
fi



# 5.4 Install postgres
if [[ -z "$opt_postgres" && -z "$opt_mysql" ]]; then
 echo_warning "Relying on an existing database installation"
fi

# before database root portion of executed code
export POSTGRES_USERNAME=${DATABASE_USERNAME}
export POSTGRES_PASSWORD=${DATABASE_PASSWORD}
if using_postgres; then
  postgres_installed=1
#  touch ${MTWILSON_HOME}/.pgpass
#  chmod 0600 ${MTWILSON_HOME}/.pgpass
#  chown ${MTWILSON_USERNAME}:${MTWILSON_USERNAME} ${MTWILSON_HOME}/.pgpass
#  if [ "$POSTGRES_HOSTNAME" == "127.0.0.1" ] || [ "$POSTGRES_HOSTNAME" == "localhost" ]; then
#    PGPASS_HOSTNAME=localhost
#  else
#    PGPASS_HOSTNAME="$POSTGRES_HOSTNAME"
#  fi
#  echo "$POSTGRES_HOSTNAME:$POSTGRES_PORTNUM:$POSTGRES_DATABASE:$POSTGRES_USERNAME:$POSTGRES_PASSWORD" > ${MTWILSON_HOME}/.pgpass
#  echo "$PGPASS_HOSTNAME:$POSTGRES_PORTNUM:$POSTGRES_DATABASE:$POSTGRES_USERNAME:$POSTGRES_PASSWORD" >> ${MTWILSON_HOME}/.pgpass
#  if [ $(whoami) == "root" ]; then cp ${MTWILSON_HOME}/.pgpass ~/.pgpass;
#  fi
   setup_pgpass
fi

# database root portion of executed code
if [ "$(whoami)" == "root" ]; then
  if using_postgres; then
    # Copy the www.postgresql.org PGP public key so add_postgresql_install_packages can add it later if needed
    if [ -d "/etc/apt" ]; then
      mkdir -p /etc/apt/trusted.gpg.d
      chmod 755 /etc/apt/trusted.gpg.d
      cp ACCC4CF8.asc "/etc/apt/trusted.gpg.d"
    fi
    POSTGRES_SERVER_APT_PACKAGES="postgresql-9.4"
    POSTGRES_SERVER_YUM_PACKAGES="postgresql94"
    if [ "$IS_RPM" != "true" ]; then
      add_postgresql_install_packages "POSTGRES_SERVER"
    fi
    if [ $? -ne 0 ]; then echo_failure "Failed to add postgresql repository to local package manager"; exit -1; fi

    postgres_userinput_connection_properties
    if [ -n "$opt_postgres" ]; then
      # Install Postgres server (if user selected localhost)
      if [[ "$POSTGRES_HOSTNAME" == "127.0.0.1" || "$POSTGRES_HOSTNAME" == "localhost" || -n `echo "$(hostaddress_list)" | grep "$POSTGRES_HOSTNAME"` ]]; then
        echo "Installing postgres server..."
        # when we install postgres server on ubuntu it prompts us for root pw
        # we preset it so we can send all output to the log
        aptget_detect; dpkg_detect; yum_detect;
        if [[ -n "$aptget" ]]; then
          echo "postgresql app-pass password $POSTGRES_PASSWORD" | debconf-set-selections
        fi
        postgres_installed=0 #postgres is being installed
        # don't need to restart postgres server unless the install script says we need to (by returning zero)
        postgres_server_install
        if [ $? -ne 0 ]; then echo_failure "Failed to install postgresql server"; exit -1; fi
        postgres_restart >> $INSTALL_LOG_FILE
        #sleep 10
        # postgres server end
      fi
      # postgres client install here
      echo "Installing postgres client..."
      if [ "$IS_RPM" != "true" ]; then
        postgres_install
      fi
      if [ $? -ne 0 ]; then echo_failure "Failed to install postgresql"; exit -1; fi
      # do not need to restart postgres server after installing the client.
      #postgres_restart >> $INSTALL_LOG_FILE
      #sleep 10
      echo "Installation of postgres client complete"
      # postgres client install end
    else
      echo_warning "Relying on an existing Postgres installation"
    fi
  fi
fi

# after database root portion of executed code
if using_postgres; then
  if [ -z "$SKIP_DATABASE_INIT" ]; then
    # postgres db init here
    postgres_create_database
    if [ $? -ne 0 ]; then
      echo_failure "Cannot create database"
      exit 1
    fi
    #postgres_restart >> $INSTALL_LOG_FILE
    #sleep 10
    #export is_postgres_available postgres_connection_error
    if [ -z "$is_postgres_available" ]; then
      echo_warning "Run 'mtwilson setup' after a database is available";
    fi
    # postgress db init end
  else
    echo_warning "Skipping init of database"
  fi
  if [ $postgres_installed -eq 0 ]; then
    postgres_server_detect
    has_local_postgres_peer=`grep "^local.*all.*postgres.*peer" $postgres_pghb_conf`
    if [ -z "$has_local_postgres_peer" ]; then
      echo "Adding PostgreSQL local 'peer' authentication for 'postgres' user..."
      sed -i '/^.*TYPE.*DATABASE.*USER.*ADDRESS.*METHOD/a local all postgres peer' $postgres_pghb_conf
    fi
    has_local_peer=`grep "^local.*all.*all.*peer" $postgres_pghb_conf`
    if [ -n "$has_local_peer" ]; then
      echo "Replacing PostgreSQL local 'peer' authentication with 'md5' authentication..."
      sed -i 's/^local.*all.*all.*peer/local all all md5/' $postgres_pghb_conf
    fi
    has_max_connections=`grep "^max_connections" $postgres_conf`
    if [ -n "$has_max_connections" ]; then
      postgres_max_connections=$(cat "$postgres_conf" 2>/dev/null | grep "^max_connections" | head -n 1 | sed 's/#.*//' | awk -F '=' '{ print $2 }' | sed -e 's/^[ \t]*//' | sed -e 's/[ \t]*$//')
      if [ -z $postgres_max_connections ] || [ $postgres_max_connections -lt 400 ]; then
        echo "Changing postgresql configuration to set max connections to 400...";
        sed -i 's/^max_connections.*/max_connections = 400/' $postgres_conf
      fi
    else
      echo "Setting postgresql max connections to 400...";
      echo "max_connections = 400" >> $postgres_conf
    fi
    has_shared_buffers=`grep "^shared_buffers" $postgres_conf`
    if [ -n "$has_shared_buffers" ]; then
      echo "Changing postgresql configuration to set shared buffers size to 400MB...";
      sed -i 's/^shared_buffers.*/shared_buffers = 400MB/' $postgres_conf
    else
      echo "Setting postgresql shared buffers size to 400MB...";
      echo "shared_buffers = 400MB" >> $postgres_conf
    fi
    if [ "$POSTGRESQL_KEEP_PGPASS" != "true" ]; then
      if [ -f ${MTWILSON_CONFIGURATION}/.pgpass ]; then
        echo "Removing .pgpass file to prevent insecure database password storage in plaintext..."
        rm -f ${MTWILSON_CONFIGURATION}/.pgpass
        if [ $(whoami) == "root" ]; then rm -f ~/.pgpass; fi
      fi
    fi
    postgres_restart >> $INSTALL_LOG_FILE
  fi
fi

########################################################################################################################
# 6. Copy scripts to application
#cp version script to configuration directory
cp version $MTWILSON_HOME/configuration/version

#cp mtwilson control script and setup symlinks
cp mtwilson.sh $MTWILSON_HOME/bin/mtwilson.sh
cp encrypt.sh $MTWILSON_HOME/bin/encrypt.sh
cp decrypt.sh $MTWILSON_HOME/bin/decrypt.sh
rm -f $MTWILSON_HOME/bin/mtwilson
ln -s $MTWILSON_HOME/bin/mtwilson.sh $MTWILSON_HOME/bin/mtwilson
chmod +x $MTWILSON_HOME/bin/*

#If user is root then create mtwilson symlink to /usr/local/bin otherwise export path '$MTWILSON_HOME/bin'
if [ "$(whoami)" == "root" ]; then
 if [ ! -d /usr/local/bin ]; then
   mkdir -p /usr/local/bin
 fi
 #Remove symbolic link if already exist
 rm -f /usr/local/bin/mtwilson
 ln -s $MTWILSON_HOME/bin/mtwilson /usr/local/bin/mtwilson
fi

# make aikverify directories, set ownership and permissions
if [ "$(whoami)" == "root" ]; then
  mkdir -p "/var/opt/intel"
fi
if [ -w "/var/opt/intel" ]; then
  mkdir -p "/var/opt/intel/aikverifyhome/bin" "/var/opt/intel/aikverifyhome/data"
  chown -R ${MTWILSON_USERNAME}:${MTWILSON_USERNAME} "/var/opt/intel"
  chmod 700 "/var/opt/intel" "/var/opt/intel/aikverifyhome/bin" "/var/opt/intel/aikverifyhome/data"
fi

mtwilson_backup_configuration() {
  if [ -n "$MTWILSON_CONFIGURATION" ] && [ -d "$MTWILSON_CONFIGURATION" ]; then
    datestr=`date +%Y%m%d.%H%M`
    backupdir="$MTWILSON_HOME/backup/mtwilson.configuration.$datestr"
    mkdir -p "$backupdir"
    cp -r $MTWILSON_CONFIGURATION $backupdir
  fi
}

mtwilson_backup_repository() {
  if [ -n "$MTWILSON_REPOSITORY" ] && [ -d "$MTWILSON_REPOSITORY" ]; then
    datestr=`date +%Y%m%d.%H%M`
    backupdir="$MTWILSON_HOME/backup/mtwilson.repository.$datestr"
    mkdir -p "$backupdir"
    cp -r $MTWILSON_REPOSITORY $backupdir
  fi
}

# backup current configuration and data, if they exist
mtwilson_backup_configuration
mtwilson_backup_repository

# store directory layout in env file
echo "# $(date)" > $MTWILSON_ENV/mtwilson-layout
echo "export MTWILSON_HOME=$MTWILSON_HOME" >> $MTWILSON_ENV/mtwilson-layout
echo "export MTWILSON_CONFIGURATION=$MTWILSON_CONFIGURATION" >> $MTWILSON_ENV/mtwilson-layout
echo "export MTWILSON_JAVA=$MTWILSON_JAVA" >> $MTWILSON_ENV/mtwilson-layout
echo "export MTWILSON_BIN=$MTWILSON_BIN" >> $MTWILSON_ENV/mtwilson-layout
echo "export MTWILSON_REPOSITORY=$MTWILSON_REPOSITORY" >> $MTWILSON_ENV/mtwilson-layout
echo "export MTWILSON_LOGS=$MTWILSON_LOGS" >> $MTWILSON_ENV/mtwilson-layout

# store mtwilson username in env file
echo "# $(date)" > $MTWILSON_ENV/mtwilson-username
echo "export MTWILSON_USERNAME=$MTWILSON_USERNAME" >> $MTWILSON_ENV/mtwilson-username

# store log level in env file, if it's set
if [ -n "$MTWILSON_LOG_LEVEL" ]; then
  echo "# $(date)" > $MTWILSON_ENV/mtwilson-logging
  echo "export MTWILSON_LOG_LEVEL=$MTWILSON_LOG_LEVEL" >> $MTWILSON_ENV/mtwilson-logging
fi

# store the auto-exported environment variables in env file
# to make them available after the script uses sudo to switch users;
# we delete that file later
echo "# $(date)" > $MTWILSON_ENV/mtwilson-setup
for env_file_var_name in $env_file_exports
do
  eval env_file_var_value="\$$env_file_var_name"
  echo "writing $env_file_var_name to mtwilson-setup with value: $env_file_var_value" >> $INSTALL_LOG_FILE
  echo "export $env_file_var_name=$env_file_var_value" >> $MTWILSON_ENV/mtwilson-setup
done

profile_dir=$HOME
if [ "$(whoami)" == "root" ] && [ -n "$MTWILSON_USERNAME" ] && [ "$MTWILSON_USERNAME" != "root" ]; then
  profile_dir=$MTWILSON_HOME
fi
profile_name=$profile_dir/$(basename $(getUserProfileFile))

echo "Updating profile: $profile_name" >> $INSTALL_LOG_FILE
appendToUserProfileFile "export PATH=$MTWILSON_BIN:\$PATH" $profile_name
appendToUserProfileFile "export MTWILSON_HOME=$MTWILSON_HOME" $profile_name

mtw_props_path="$MTWILSON_CONFIGURATION/mtwilson.properties"
as_props_path="$MTWILSON_CONFIGURATION/attestation-service.properties"
file_paths=("$mtw_props_path" "$as_props_path")

mtwilson_password_file="$MTWILSON_CONFIGURATION/.mtwilson_password"
if [ -f "$mtwilson_password_file" ]; then
  export MTWILSON_PASSWORD=$(cat $mtwilson_password_file)
fi

#echo "Loading configuration settings and defaults" >> $INSTALL_LOG_FILE
#load_conf
#load_defaults

# Java configurations
JAVA_CMD=$(type -p java | xargs readlink -f)
JAVA_HOME=$(dirname $JAVA_CMD | xargs dirname | xargs dirname)
JAVA_REQUIRED_VERSION=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')
# store java location in env file
echo "# $(date)" > $MTWILSON_ENV/mtwilson-java
echo "export JAVA_HOME=$JAVA_HOME" >> $MTWILSON_ENV/mtwilson-java
echo "export JAVA_CMD=$JAVA_CMD" >> $MTWILSON_ENV/mtwilson-java
echo "export JAVA_REQUIRED_VERSION=$JAVA_REQUIRED_VERSION" >> $MTWILSON_ENV/mtwilson-java

if [ -f "${JAVA_HOME}/jre/lib/security/java.security" ]; then
  echo "Replacing java.security file, existing file will be backed up"
  backup_file "${JAVA_HOME}/jre/lib/security/java.security"
  cp java.security "${JAVA_HOME}/jre/lib/security/java.security"
fi

MTWILSON_PORT_HTTP=${MTWILSON_PORT_HTTP:-${JETTY_PORT:-8442}}
MTWILSON_PORT_HTTPS=${MTWILSON_PORT_HTTPS:-${JETTY_SECURE_PORT:-8443}}

# delete existing java files, to prevent a situation where the installer copies
# a newer file but the older file is also there
if [ -d $MTWILSON_JAVA ]; then
  rm $MTWILSON_JAVA/*.jar 2>/dev/null
fi

# if a group is defined, then make all sub parts == true
if [ ! -z "$opt_services" ]; then
  eval opt_attservice="true"
  eval opt_mangservice="true"
  eval opt_wlmservice="true"
fi

# 7. extract mtwilson
echo "Extracting application..."
MTWILSON_ZIPFILE=`ls -1 host-verification-service-zip-*.zip 2>/dev/null | tail -n 1`
unzip -oq $MTWILSON_ZIPFILE -d $MTWILSON_HOME >>$INSTALL_LOG_FILE 2>&1

# deprecated:  remove when references have been updated to $MTWILSON_HOME/share/scripts/functions.sh
cp functions "$MTWILSON_BIN/functions.sh"

# copy logging configurations
chmod 600 logback.xml logback-stderr.xml log4j.properties
cp logback.xml logback-stderr.xml log4j.properties "${MTWILSON_CONFIGURATION}"

# set permissions
echo "chown -R $MTWILSON_USERNAME:$MTWILSON_USERNAME $MTWILSON_HOME" >> $INSTALL_LOG_FILE
chown -R $MTWILSON_USERNAME:$MTWILSON_USERNAME $MTWILSON_HOME
chmod 755 $MTWILSON_BIN/*

# 8. configure mtwilson TLS policies
echo "Configuring TLS policies..." >>$INSTALL_LOG_FILE
if [ -f "$MTWILSON_CONFIGURATION/mtwilson.properties" ]; then
  #default_mtwilson_tls_policy_id="$MTWILSON_DEFAULT_TLS_POLICY_ID"
  default_mtwilson_tls_policy_id="${MTWILSON_DEFAULT_TLS_POLICY_ID:-$MTW_DEFAULT_TLS_POLICY_ID}"   #`read_property_from_file "mtwilson.default.tls.policy.id" /etc/intel/cloudsecurity/mtwilson.properties`
  if [ "$default_mtwilson_tls_policy_id" == "INSECURE" ] || [ "$default_mtwilson_tls_policy_id" == "TRUST_FIRST_CERTIFICATE" ]; then
    echo_warning "Default TLS policy is insecure; the product guide contains information on enabling secure TLS policies"
  fi
  #fi
  export MTWILSON_TLS_KEYSTORE_PASSWORD="${MTWILSON_TLS_KEYSTORE_PASSWORD:-$MTW_TLS_KEYSTORE_PASS}"   #`read_property_from_file "mtwilson.tls.keystore.password" /etc/intel/cloudsecurity/mtwilson.properties`
else
  touch "$MTWILSON_CONFIGURATION/mtwilson.properties"
  chmod 600 "$MTWILSON_CONFIGURATION/mtwilson.properties"
  chown $MTWILSON_USERNAME:$MTWILSON_USRENAME "$MTWILSON_CONFIGURATION/mtwilson.properties"
  export MTWILSON_TLS_KEYSTORE_PASSWORD=`generate_password 32`
  echo '#mtwilson.default.tls.policy.id=uuid of a shared policy or INSECURE or TRUST_FIRST_CERTIFICATE for Mt Wilson 1.2 behavior' >>  "$MTWILSON_CONFIGURATION/mtwilson.properties"
  echo '#mtwilson.global.tls.policy.id=uuid of a shared policy or INSECURE or TRUST_FIRST_CERTIFICATE for Mt Wilson 1.2 behavior' >>  "$MTWILSON_CONFIGURATION/mtwilson.properties"
  # NOTE: do not change this property once it exists!  it would lock out all hosts that are already added and prevent mt wilson from getting trust status
  # in a future release we will have a UI mechanism to manage this.
fi

#MTWILSON_TLS_POLICY_ALLOW
MTWILSON_TLS_POLICY_ALLOW=`echo $MTWILSON_TLS_POLICY_ALLOW | tr -d ' '`   # trim whitespace
OIFS=$IFS
IFS=',' read -ra POLICIES <<< "$MTWILSON_TLS_POLICY_ALLOW"
IFS=$OIFS
TMP_MTWILSON_TLS_POLICY_ALLOW=
for i in "${POLICIES[@]}"; do
  if [ "$i" == "certificate" ] || [ "$i" == "certificate-digest" ] || [ "$i" == "public-key" ] || [ "$i" == "public-key-digest" ] || [ "$i" == "TRUST_FIRST_CERTIFICATE" ] || [ "$i" == "INSECURE" ]; then
    TMP_MTWILSON_TLS_POLICY_ALLOW+="$i,"
  fi
done
MTWILSON_TLS_POLICY_ALLOW=`echo "$TMP_MTWILSON_TLS_POLICY_ALLOW" | sed 's/\(.*\),/\1/'`

if [ -n "$MTWILSON_TLS_POLICY_ALLOW" ]; then
  mtwilson config "mtwilson.tls.policy.allow" "$MTWILSON_TLS_POLICY_ALLOW" >/dev/null
else
  echo_failure "An allowed TLS policy must be defined."
  exit -1
fi

#MTWILSON_DEFAULT_TLS_POLICY_ID
MTWILSON_DEFAULT_TLS_POLICY_ID=`echo $MTWILSON_DEFAULT_TLS_POLICY_ID | tr -d ' '`   # trim whitespace
OIFS=$IFS
IFS=',' read -ra POLICIES <<< "$MTWILSON_TLS_POLICY_ALLOW"
IFS=$OIFS
TMP_MTWILSON_DEFAULT_TLS_POLICY_ID=
for i in "${POLICIES[@]}"; do
  if [ "$i" == "$MTWILSON_DEFAULT_TLS_POLICY_ID" ]; then
    TMP_MTWILSON_DEFAULT_TLS_POLICY_ID="$i"
  fi
done
MTWILSON_DEFAULT_TLS_POLICY_ID=`echo "$TMP_MTWILSON_DEFAULT_TLS_POLICY_ID"`

if [[ "$MTWILSON_DEFAULT_TLS_POLICY_ID" == "INSECURE" || "$MTWILSON_DEFAULT_TLS_POLICY_ID" == "TRUST_FIRST_CERTIFICATE" ]]; then
  mtwilson config "mtwilson.default.tls.policy.id" "$MTWILSON_DEFAULT_TLS_POLICY_ID" >/dev/null
else
  echo_warning "Unable to determine default TLS policy."
#  exit -1
fi

########################################################################################################################
# 9. Update configurations
#Database config
mtwilson config "mtwilson.db.host" "${DATABASE_HOSTNAME}" >/dev/null
mtwilson config "mtwilson.db.port" "${DATABASE_PORTNUM}" >/dev/null
mtwilson config "mtwilson.db.schema" "${DATABASE_SCHEMA}" >/dev/null
mtwilson config "mtwilson.db.user" "${DATABASE_USERNAME}" >/dev/null
mtwilson config "mtwilson.db.password" "${DATABASE_PASSWORD}" >/dev/null

#export AUTO_UPDATE_ON_UNTRUST=${AUTO_UPDATE_ON_UNTRUST:-false}
#mtwilson config "mtwilson.as.autoUpdateHost" "$AUTO_UPDATE_ON_UNTRUST" >/dev/null
mtwilson config "mtwilson.locales" "en-US" >/dev/null

# Queue Execution Interval configuration parameter (in seconds)
export QUEUE_EXECUTION_INTERVAL=${QUEUE_EXECUTION_INTERVAL:-3}
mtwilson config "mtwilson.queue.execution.interval" "$QUEUE_EXECUTION_INTERVAL" >/dev/null

# Client connection timeout configuration parameter (in seconds)
export CLIENT_CONNECTION_TIMEOUT=${CLIENT_CONNECTION_TIMEOUT:-10}
mtwilson config "mtwilson.config.client.connectTimeout" "$CLIENT_CONNECTION_TIMEOUT" >/dev/null

# Client read timeout configuration parameter (in seconds)
export CLIENT_READ_TIMEOUT=${CLIENT_READ_TIMEOUT:-10}
mtwilson config "mtwilson.config.client.readTimeout" "$CLIENT_READ_TIMEOUT" >/dev/null

# ESXI Host Auto Registration Interval configuration parameter (in seconds)
export ESXIHOSTS_AUTOUPDATE_INTERVAL=${ESXIHOSTS_AUTOUPDATE_INTERVAL:-120}
mtwilson config "mtwilson.esxihosts.autoupdate.interval" "$ESXIHOSTS_AUTOUPDATE_INTERVAL" >/dev/null

# Telemetry Interval configuration parameter (in seconds) 86400s = 1 day
export MTWILSON_TELEMETRY_INTERVAL=${MTWILSON_TELEMETRY_INTERVAL:-86400}
mtwilson config "mtwilson.telemetry.interval" "$MTWILSON_TELEMETRY_INTERVAL" >/dev/null

# Host-Verification Service audit log table size configuration parameter 
export MTWILSON_AUDIT_LOG_MAX_ROW_COUNT=${MTWILSON_AUDIT_LOG_MAX_ROW_COUNT:-1000000}
mtwilson config "mtwilson.audit.log.max.row.count" "$MTWILSON_AUDIT_LOG_MAX_ROW_COUNT" >/dev/null

# Host-Verification Service audit log number of rotations configuration parameter 
export MTWILSON_AUDIT_LOG_NUM_ROTATIONS=${MTWILSON_AUDIT_LOG_NUM_ROTATIONS:-10}
mtwilson config "mtwilson.audit.log.num.rotations" "$MTWILSON_AUDIT_LOG_NUM_ROTATIONS" >/dev/null

# SAML configuration parameters (these are static ones). The issuer and passwords
# are written by asctl.sh
export SAML_KEYSTORE_FILE=${SAML_KEYSTORE_FILE:-SAML.p12}
mtwilson config "saml.keystore.file" "$SAML_KEYSTORE_FILE" >/dev/null
export SAML_KEY_ALIAS=${SAML_KEY_ALIAS:-samlkey1}
mtwilson config "saml.key.alias" "$SAML_KEY_ALIAS" >/dev/null
export SAML_VALIDITY_SECONDS=${SAML_VALIDITY_SECONDS:-86400}
mtwilson config "saml.validity.seconds" "$SAML_VALIDITY_SECONDS" >/dev/null

#Save variables to properties file
postgres_write_connection_properties "$MTWILSON_CONFIGURATION/mtwilson.properties" mtwilson.db

# default connection pool settings
#mtwilson config "dbcp.validation.query" "select 1" >/dev/null   ### NEED TO FIX: config to accept parameters with spaces
update_property_in_file "dbcp.validation.query" "$MTWILSON_CONFIGURATION/mtwilson.properties" "select 1"
mtwilson config "dbcp.validation.on.borrow" "true" >/dev/null
mtwilson config "dbcp.validation.on.return" "false" >/dev/null

# Gather default configuration
MTWILSON_SERVER_IP_ADDRESS=${MTWILSON_SERVER_IP_ADDRESS:-$(hostaddress)}

# validate mtwilson server
for h in $(hostaddress_list); do echo "+ $h"; done; echo "+ "`hostname`
if [ -z "MTWILSON_SERVER" ]; then
  echo_warning "MTWILSON_SERVER details missing in properties";
  exit -1;
fi
export MTWILSON_SERVER
echo

# Attestation service auto-configuration
export PRIVACYCA_SERVER=$MTWILSON_SERVER

# copy shiro.ini api security file
if [ ! -f "$MTWILSON_CONFIGURATION/shiro.ini" ]; then
  echo "Copying shiro.ini to $MTWILSON_CONFIGURATION" >> $INSTALL_LOG_FILE
  chmod 600 shiro.ini shiro-localhost.ini
  cp shiro.ini shiro-localhost.ini "$MTWILSON_CONFIGURATION"
fi
chown -R $MTWILSON_USERNAME:$MTWILSON_USERNAME "${MTWILSON_CONFIGURATION}"

echo "Adding $MTWILSON_SERVER to shiro.ini..." >>$INSTALL_LOG_FILE
# add MTWILSON_SERVER to shiro trust file
# use "hostFilter.allow" when using the access-denying filter (any clients not from that list of ip's will be denied)
# use "iniHostRealm.allow" when using the access-allowing filter (any clients from that list of ip's will be allowed access but clients from other ip's can still try password or x509 authentication) - this is the current default
hostAllowPropertyName=iniHostRealm.allow
sed -i '/'"$hostAllowPropertyName"'/ s/^#//g' "$MTWILSON_CONFIGURATION/shiro.ini"
hostAllow=`read_property_from_file $hostAllowPropertyName "$MTWILSON_CONFIGURATION/shiro.ini"`
if [[ $hostAllow != *$MTWILSON_SERVER* ]]; then
  update_property_in_file "$hostAllowPropertyName" "$MTWILSON_CONFIGURATION/shiro.ini" "$hostAllow,$MTWILSON_SERVER"
fi
hostAllow=`read_property_from_file $hostAllowPropertyName "$MTWILSON_CONFIGURATION/shiro.ini"`
if [[ $hostAllow != *$MTWILSON_SERVER_IP_ADDRESS* ]]; then
  update_property_in_file "$hostAllowPropertyName" "$MTWILSON_CONFIGURATION/shiro.ini" "$hostAllow,$MTWILSON_SERVER_IP_ADDRESS"
fi
sed -i '/'"$hostAllowPropertyName"'/ s/^\([^#]\)/#\1/g' "$MTWILSON_CONFIGURATION/shiro.ini"

# This property is needed by the UpdateSslPort command to determine the port # that should be used in the shiro.ini file
MTWILSON_API_BASEURL="https://${MTWILSON_SERVER}:${MTWILSON_PORT_HTTPS}/mtwilson/v1"
mtwilson config "mtwilson.api.url" "$MTWILSON_API_BASEURL" >/dev/null

# Make sure the nodeploy flag is cleared, so service setup commands will deploy their .war files
export MTWILSON_SETUP_NODEPLOY=

# Configure tag
export MTWILSON_TAG_CERT_IMPORT_AUTO=${MTWILSON_TAG_CERT_IMPORT_AUTO:-true}
# export TAG_PROVISION_EXTERNAL=${MTWILSON_TAG_CERT_IMPORT_AUTO:-false}
# mtwilson config "tag.provision.external" "$TAG_PROVISION_EXTERNAL" >/dev/null
# export TAG_PROVISION_XML_ENCRYPTION_REQUIRED=${TAG_PROVISION_XML_ENCRYPTION_REQUIRED:-false}
# mtwilson config "tag.provision.xml.encryption.required" "$TAG_PROVISION_XML_ENCRYPTION_REQUIRED" >/dev/null
# export TAG_PROVISION_XML_PASSWORD=${TAG_PROVISION_XML_PASSWORD:-TagProvisionPassword}
# mtwilson config "tag.provision.xml.encryption.password" "$TAG_PROVISION_XML_PASSWORD" >/dev/null
#mtwilson config "tag.provision.selection.default" "$TAG_PROVISION_SELECTION_DEFAULT" >/dev/null
export TAG_VALIDITY_SECONDS=${TAG_VALIDITY_SECONDS:-31536000}
mtwilson config "tag.validity.seconds" "$TAG_VALIDITY_SECONDS" >/dev/null
export TAG_ISSUER_DN=${TAG_ISSUER_DN:-'CN=mtwilson-tag-ca'}
mtwilson config "tag.issuer.dn" "$TAG_ISSUER_DN" >/dev/null

chmod +x *.bin

mkdir -p /opt/mtwilson/logs
touch /opt/mtwilson/logs/mtwilson.log
touch /opt/mtwilson/logs/mtwilson-audit.log
chown -R $MTWILSON_USERNAME:$MTWILSON_USERNAME $MTWILSON_HOME

# use of "mtwilson config" method will be required when mtwilson setup is 
# revised to use the "mtwilson" command itself for java setup tasks and
# when the "mtwilson" command automatically switches to the "mtwilson" user
# because then it won't have access to the environment variables.
## mtwilson config mtwilson.extensions.fileIncludeFilter.contains "${MTWILSON_EXTENSIONS_FILEINCLUDEFILTER_CONTAINS:-'mtwilson'}" >/dev/null
export MTWILSON_EXTENSIONS_FILEINCLUDEFILTER_CONTAINS=${MTWILSON_EXTENSIONS_FILEINCLUDEFILTER_CONTAINS:-'mtwilson'}
mtwilson setup update-extensions-cache-file --force 2> /dev/null

set_owner_for_mtwilson_directories

# 10. ASCTL SETUP
############################################################# ASCTL SETUP START #############################################################

# create and setup database
if using_postgres; then
  if [ ! -n "$psql" ]; then
    echo "psql not defined"
    exit 1
  fi
fi

############################################################# ASCTL SETUP END #############################################################

# 11. setup the director, unless the NOSETUP variable is defined
if [ -z "$MTWILSON_NOSETUP" ]; then
  mtwilson setup
fi

set_owner_for_mtwilson_directories
mtwilson start

########################################################################################################################
# 12. tag service installation

#call create-database
#mtwilson setup initialize-db --force

#mtwilson tag-init-database
#mtwilson tag-create-ca-key "CN=assetTagService"
#mtwilson tag-export-file cacerts | grep -v ":" >> ${MTWILSON_CONFIGURATION}/tag-cacerts.pem

#for tag encryption
mkdir -p /opt/mtwilson/features/tag/var
mkdir -p /opt/mtwilson/features/tag/bin
cp encrypt.sh /opt/mtwilson/features/tag/bin
cp decrypt.sh /opt/mtwilson/features/tag/bin
chmod 755 /opt/mtwilson/features/tag/bin/encrypt.sh
chmod 755 /opt/mtwilson/features/tag/bin/decrypt.sh

########################################################################################################################
# 13. config logrotate
mkdir -p /etc/logrotate.d

if [ ! -a /etc/logrotate.d/mtwilson ]; then
 echo "/opt/mtwilson/logs/mtwilson.log {
    missingok
	notifempty
	rotate $LOG_OLD
	maxsize $LOG_SIZE
    nodateext
	$LOG_ROTATION_PERIOD 
	$LOG_COMPRESS
	$LOG_DELAYCOMPRESS
	$LOG_COPYTRUNCATE
}" > /etc/logrotate.d/mtwilson
fi
checkmodule -M -m mylogrotate.te -o /tmp/mylogrotate.mod
semodule_package -m /tmp/mylogrotate.mod -o /tmp/mylogrotate.pp
semodule -i /tmp/mylogrotate.pp
rm -rf /tmp/mylogrotate.mod /tmp/mylogrotate.pp




set_owner_for_mtwilson_directories

# store server hostname or ip address (whatever user configured) for server
# to use when constructing self-references
mtwilson config mtwilson.host "$MTWILSON_SERVER" >/dev/null

# delete the temporary setup environment variables file
rm -f $MTWILSON_ENV/mtwilson-setup
rm -f ~/.pgpass

# 14. Register mtwilson as a startup script
# RHEL 7.6 needs PID file for systemd startup service
# The location is identified as below in mtwilson.sh
MTWILSON_PID_FILE=$MTWILSON_HOME/mtwilson.pid

register_startup_script /usr/local/bin/mtwilson mtwilson $MTWILSON_PID_FILE

# last chance to set permissions
chmod 600 "$MTWILSON_CONFIGURATION/*.properties" 2>/dev/null
chmod 700 "/var/opt/intel" "/var/opt/intel/aikverifyhome/bin" "/var/opt/intel/aikverifyhome/data"

# 15. Start mtwilson service
echo "Restarting webservice for all changes to take effect"
mtwilson restart

echo "Log file for install is located at $INSTALL_LOG_FILE"
if [ -n "$INSTALLED_MARKER_FILE" ]; then
 touch $INSTALLED_MARKER_FILE
fi
if [ "$(whoami)" != "root" ]; then 
  echo_warning "Please relogin to use mtwilson utilities"
fi
