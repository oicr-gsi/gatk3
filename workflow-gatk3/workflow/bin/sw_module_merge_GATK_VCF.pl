use strict;
use Data::Dumper;
use Getopt::Long;

##########
#
# Script:  sw_module_merge_GATK_VCF.pl
# Date:    20110808
# Author:  Brian O'Connor <briandoconnor@gmail.com>
#
# Purpose: Takes two or more VCF files from GATK and combines them into one VCF.
#          
# Input:   one or more VCF version 4.1 from GATK
#
# Output:  a single VCF 4.1 that's been merged, no assumption is made on how to sort this file
# 
##########

my (@vcf_files, $vcf_out, $help);
$help = 0;
my $argSize = scalar(@ARGV);
my $getOptResult = GetOptions('vcf-input-file=s' => \@vcf_files, 'vcf-output-file=s' => \$vcf_out, 'help' => \$help);
usage() if ( $argSize < 4 || !$getOptResult || $help);

###########################################################################################################################

my $h = {};
my $vcf_version = "";
my $main_header = "";

foreach my $file (@vcf_files) {
  open IN, "<$file" or die "Can't open $file\n";
  while(<IN>) {
    chomp;
    last if (!/^#/);
    if (/^##INFO/ || /^##FILTER/ || /^##FORMAT/) {
      $h->{$_} = 1;
    }
    if (/^##fileformat=/) { $vcf_version = $_; }
    if (/^#CHROM/) { $main_header = $_; }
  }
  close IN;
}

open OUT, ">$vcf_out" or die "can't open $vcf_out\n";
print OUT "$vcf_version\n";
foreach my $header (sort keys %{$h}) {
  print OUT "$header\n";
}
print OUT "$main_header\n";
foreach my $file (@vcf_files) {
  open IN, "<$file" or die "Can't open $file\n";
  while(<IN>) {
    if (!/^#/) { print OUT $_; }
  }
  close IN;
}
close OUT;

exit(0);

sub usage {
  print "Unknown option: @_\n" if ( @_ );
  print "usage: program [--vcf-input-file INPUT_FILE] [--vcf-output-file OUTPUT] [[--help|-?]\n";
  exit(1);
}

