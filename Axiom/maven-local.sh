mvn install:install-file -Dfile="../../axiom-jars/microba-0.4.4.jar" -DgroupId="com.michaelbaranov" -DartifactId=microba -Dversion="0.4.4" -Dpackaging=jar

mvn install:install-file -Dfile="../../axiom-jars/l2fprod-common-all.jar" -DgroupId="com.l2fprod.common" -DartifactId=l2fprod-common-all -Dversion="7.3.0" -Dpackaging=jar

mvn install:install-file -Dfile="../../axiom-jars/swing-layout-1.0.4.jar" -DgroupId="org.jdesktop" -DartifactId=swing-layout -Dversion="1.0.4" -Dpackaging=jar
