<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;
use Illuminate\Support\Str;

/**
 * @property int    $Mn_id
 * @property int    $created_at
 * @property int    $updated_at
 * @property int    $deleted_at
 * @property string $Mn_name
 * @property Date   $Mn_date
 */
class MNews extends Model
{
    use SoftDeletes;
    /**
     * The database table used by the model.
     *
     * @var string
     */
    protected $table = 'm_news';

    /**
     * The primary key for the model.
     *
     * @var string
     */
    protected $primaryKey = 'Mn_id';

    /**
     * Attributes that should be mass-assignable.
     *
     * @var array
     */
    protected $fillable = [
        'Mn_type', 'Mn_name', 'Mn_date', 'Mn_embed_video', 'Mn_pin', 'Mn_order', 'created_at', 'updated_at', 'deleted_at'
    ];

    /**
     * The attributes excluded from the model's JSON form.
     *
     * @var array
     */
    protected $hidden = [];

    /**
     * The attributes that should be casted to native types.
     *
     * @var array
     */
    protected $casts = [
        'Mn_id' => 'int', 'Mn_type' => 'int', 'Mn_name' => 'string', 'Mn_date' => 'date', 'Mn_embed_video' => 'string', 'Mn_pin' => 'int', 'Mn_order' => 'int', 'created_at' => 'timestamp', 'updated_at' => 'timestamp', 'deleted_at' => 'timestamp'
    ];

    /**
     * The attributes that should be mutated to dates.
     *
     * @var array
     */
    protected $dates = [
        'Mn_date', 'created_at', 'updated_at', 'deleted_at'
    ];

    /**
     * Indicates if the model should be timestamped.
     *
     * @var boolean
     */
    public $timestamps = false;

    public static function boot()
    {
        parent::boot();

        static::creating(function ($article) {
            $article->created_at = now();
            $article->updated_at = now();
        });

        static::saving(function ($article) {
            $article->updated_at = now();
        });


        self::created(function ($article) {
            $truncated = substr($article->Mn_name, 0, 80) . "-" . $article->Mn_id;
            MNewsLng::create([
                'Mn_id' => $article->Mn_id,
                'S_Lng_id' => 1,
                'MnL_path' =>  Str::slug(C2L($truncated)),
                'MnL_title' => $article->Mn_name,
            ]);
        });
    }

    // Scopes...

    // Functions ...

    // Relations ...

    public function eq_lng()
    {
        return $this->hasMany(MNewsLng::class, 'Mn_id');
    }

    public function eq_gallery()
    {
        return $this->hasMany(MGallery::class, 'Mn_id');
    }
}
