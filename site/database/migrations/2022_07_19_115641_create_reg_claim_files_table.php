<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateRegClaimFilesTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('reg_claim_files', function (Blueprint $table) {
            $table->increments('R_ClF_id');
            $table->integer('R_Cl_id')->nullable()->unsigned();

            $table->string('R_ClF_name', 400)->nullable();
            $table->string('R_ClF_file', 400)->nullable();
            $table->decimal('R_ClF_size', 8, 2)->nullable();



            $table->timestamps();
            $table->softDeletes();

            $table->foreign('R_Cl_id')->references('R_Cl_id')->on('reg_claim');
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::dropIfExists('reg_claim_files');
    }
}
