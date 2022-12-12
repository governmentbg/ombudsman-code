<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateMPositionTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('m_position', function (Blueprint $table) {
            $table->increments('Pst_id');


            $table->date('Pst_date', 150)->nullable();
            $table->text('Pst_body')->nullable();
            $table->string('Pst_file', 150)->nullable();
            $table->string('Pst_name', 650)->nullable();
            $table->string('Pst_size', 20)->nullable();
            $table->string('Pst_type', 20)->nullable();
            $table->string('Pst_desc', 500)->nullable();


            $table->timestamps();
            $table->softDeletes();
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::dropIfExists('m_position');
    }
}
