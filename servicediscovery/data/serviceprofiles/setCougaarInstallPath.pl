########################################
# Usage is setCougaarInstallPath.pl <yourpath> <fakepath>.
# <fakepath> is the path you want to replace, which is C:/cougaar
# In Windows, yourpath is something like C:/cougaar.
# Be sure to use a capital letter for drive, and "/", not "\".
# In Linux, yourpath is something like /cougaar.
#
#######################################



sub setup_files{
    @xsbAgents = ("DLA-AVIATION", DRYTECH, GRAINGER, NEWARK, PARTSALACARTE, PWCSANDIEGO, WARNERROBBINS, CCAD);
    @xsbWsdls = (DLA-AVIATION, DRYTECH, PARTSALACARTE, PWCSANDIEGO, WARNERROBBINS, CCAD);
}

sub usage {
    &error_out("Usage: setCougaarInstallPath.pl <realCougaarInstallPath> <fakeCougaarInstallPath>.");
} # usage

sub error_out {
    foreach (@_) {
	print STDERR $_."\n";
    }
    exit(1);
} # error_out

sub read_command_args {
    &usage if $#ARGV!=1;

    if($_=shift(@ARGV)) {
	&usage if /-?-h/;
	$COUGAAR_INSTALL_PATH=$_;
    }
    if($_=shift(@ARGV)){
	$REPLACEMENT_TARGET_STRING=$_;
    }
    return($COUGAAR_INSTALL_PATH, $REPLACEMENT_TARGET_STRING);
} # read_command_args


sub main {

    ($COUGAAR_INSTALL_PATH, $REPLACEMENT_TARGET_STRING)=&read_command_args;

    print "Replace ".$REPLACEMENT_TARGET_STRING." with ".$COUGAAR_INSTALL_PATH."\n";

    &setup_files;

    foreach $xsbAgents (@xsbAgents){
	print $xsbAgents."\n";
	open(PROFILE, "$xsbAgents.profile.daml") || &error_out("Can't open file $xsbAgents.profile.daml for reading.");
#	print "reading\n";
	while(<PROFILE>){
#	    print $_;
	    push @data, $_;
	}
	close(PROFILE);

	open(PROFILE, ">$xsbAgents.profile.daml") || &error_out("Can't open file $xsbAgents.profile.daml for writing.");
#	print "writing\n";
	while($_=shift(@data)){
	    s/$REPLACEMENT_TARGET_STRING/$COUGAAR_INSTALL_PATH/;
	    print PROFILE $_;	   
#	    print $_;
	}
	close(PROFILE);
    }

}

&main;
